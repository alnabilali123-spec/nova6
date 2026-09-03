package com.novatube.app.download

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.novatube.app.NovaTubeApp
import com.novatube.app.R
import com.novatube.app.data.model.RequestedDownload
import com.novatube.app.service.DownloadService
import com.novatube.app.util.FileUtils
import com.novatube.app.util.NotificationHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Owns a single download run. Loads the [com.novatube.app.data.entity.DownloadEntity] from Room,
 * runs `yt-dlp` via [com.novatube.app.core.YtDlpEngine], and updates the entity as the
 * download progresses.
 *
 * The engine invokes progress callbacks from a non-suspending thread. We bridge that
 * to suspend-only Room calls via a [Channel] drained by a coroutine we launch here.
 */
class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as NovaTubeApp
        val id = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        if (id < 0) return Result.failure(workDataOf("error" to "missing id"))

        val entity = app.downloadRepository.get(id)
            ?: return Result.failure(workDataOf("error" to "no row"))
        val request = RequestedDownload(
            url = entity.webpageUrl ?: entity.url,
            formatId = entity.formatId,
            fileName = entity.fileName.substringBeforeLast('.', entity.fileName),
            isAudioOnly = entity.isAudioOnly,
            audioFormat = entity.audioFormat ?: "mp3",
            title = entity.title,
            uploader = entity.uploader,
            thumbnail = entity.thumbnail,
            duration = entity.duration,
            webpageUrl = entity.webpageUrl,
            embedSubtitles = entity.embedSubtitles,
            subtitleLang = entity.subtitleLang
        )

        setForeground(createForegroundInfo(entity.title, 0))
        app.downloadRepository.markRunning(id)

        val completedSignal = CompletableDeferred<File?>()
        val outputDir = if (request.isAudioOnly) FileUtils.audioDir(applicationContext) else FileUtils.downloadDir(applicationContext)
        outputDir.mkdirs()

        // Channel to bridge non-suspend progress callbacks to our coroutine context.
        val progressChannel = Channel<com.novatube.app.core.ProgressEvent>(Channel.UNLIMITED)
        val progressScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val progressJob = progressScope.launch {
            for (event in progressChannel) {
                val percent = event.percent.toInt().coerceIn(0, 100)
                runCatching {
                    app.downloadRepository.updateProgress(
                        id,
                        percent,
                        event.downloadedBytes,
                        event.speed,
                        event.etaSeconds
                    )
                }.onFailure { Log.w(TAG, "updateProgress failed", it) }
                runCatching {
                    setProgress(
                        workDataOf(
                            KEY_PROGRESS to percent,
                            KEY_BYTES to event.downloadedBytes,
                            KEY_TOTAL to event.totalBytes,
                            KEY_SPEED to event.speed
                        )
                    )
                }.onFailure { Log.w(TAG, "setProgress failed", it) }
                runCatching {
                    setForeground(createForegroundInfo(entity.title, percent))
                }.onFailure { Log.w(TAG, "setForeground failed", it) }
                DownloadService.broadcastProgress(applicationContext, id, percent, entity.title)
            }
        }

        val result = withTimeoutOrNull(6 * 60 * 60 * 1000L) {
            app.ytDlpEngine.download(
                url = request.url,
                outputDir = outputDir,
                filename = request.fileName.substringBeforeLast('.', request.fileName),
                formatId = request.formatId,
                audioOnly = request.isAudioOnly,
                audioFormat = request.audioFormat,
                embedMetadata = true,
                embedThumbnail = request.isAudioOnly,
                writeSubtitles = request.embedSubtitles,
                subtitleLang = request.subtitleLang,
                onProgress = { event -> progressChannel.trySend(event) }
            ).also {
                if (it != null) completedSignal.complete(it)
                else completedSignal.completeExceptionally(RuntimeException("yt-dlp returned null"))
            }
            try { completedSignal.await() } catch (e: Exception) { null }
        }

        progressChannel.close()
        progressJob.join()
        progressScope.cancel()

        return if (result != null) {
            val file = result
            val size = file.length()
            app.downloadRepository.markCompleted(id, file.absolutePath, size)
            DownloadService.broadcastComplete(applicationContext, id, entity.title, file.absolutePath)
            runCatching { setProgress(workDataOf(KEY_PROGRESS to 100, KEY_TOTAL to size, KEY_BYTES to size)) }
            Result.success(workDataOf("path" to file.absolutePath, "size" to size))
        } else {
            val msg = when {
                isStopped -> "Cancelled"
                !app.ytDlpEngine.state.value.ready -> "Engine not ready: ${app.ytDlpEngine.state.value.lastError ?: "unknown"}"
                else -> "Timed out"
            }
            app.downloadRepository.markFailed(id, msg)
            DownloadService.broadcastFailed(applicationContext, id, entity.title, msg)
            Result.failure(workDataOf("error" to msg))
        }
    }

    private fun createForegroundInfo(title: String, percent: Int): ForegroundInfo {
        val notification = buildNotification(title, percent)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(title: String, percent: Int): Notification {
        val cancelIntent = DownloadService.cancelIntent(applicationContext)
        val pi = android.app.PendingIntent.getService(
            applicationContext, 0, cancelIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_DOWNLOADS)
            .setContentTitle(applicationContext.getString(R.string.notif_downloading, title))
            .setContentText(applicationContext.getString(R.string.notif_progress, percent))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, percent == 0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, applicationContext.getString(R.string.common_cancel), pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun workDataOf(vararg pairs: Pair<String, Any?>): Data {
        val b = Data.Builder()
        pairs.forEach { (k, v) ->
            if (v == null) b.putString(k, null) else when (v) {
                is String -> b.putString(k, v)
                is Int -> b.putInt(k, v)
                is Long -> b.putLong(k, v)
                is Float -> b.putFloat(k, v)
                is Double -> b.putDouble(k, v)
                is Boolean -> b.putBoolean(k, v)
                else -> b.putString(k, v.toString())
            }
        }
        return b.build()
    }

    companion object {
        private const val TAG = "DownloadWorker"
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_BYTES = "bytes"
        const val KEY_TOTAL = "total"
        const val KEY_SPEED = "speed"
        private const val NOTIF_ID = 1001
    }
}
