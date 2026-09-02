package com.novatube.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.novatube.app.NovaTubeApp
import com.novatube.app.R
import com.novatube.app.util.NotificationHelper

/**
 * Broadcasts download progress / completion / failure events to in-app collectors.
 * Also exposes a [cancelIntent] builder so a foreground service notification can
 * stop a running download.
 */
object DownloadService {

    const val ACTION_PROGRESS = "com.novatube.app.DOWNLOAD_PROGRESS"
    const val ACTION_COMPLETE = "com.novatube.app.DOWNLOAD_COMPLETE"
    const val ACTION_FAILED = "com.novatube.app.DOWNLOAD_FAILED"
    const val EXTRA_ID = "id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_PROGRESS = "progress"
    const val EXTRA_PATH = "path"
    const val EXTRA_ERROR = "error"

    fun cancelIntent(context: Context, downloadId: Long? = null): Intent =
        Intent(context, DownloadServiceImpl::class.java).apply {
            action = ACTION_CANCEL
            if (downloadId != null) putExtra(EXTRA_ID, downloadId)
        }

    fun broadcastProgress(context: Context, id: Long, percent: Int, title: String) {
        val intent = Intent(ACTION_PROGRESS).apply {
            putExtra(EXTRA_ID, id); putExtra(EXTRA_PROGRESS, percent); putExtra(EXTRA_TITLE, title)
        }
        context.sendBroadcast(intent)
    }

    fun broadcastComplete(context: Context, id: Long, title: String, path: String) {
        val intent = Intent(ACTION_COMPLETE).apply {
            putExtra(EXTRA_ID, id); putExtra(EXTRA_TITLE, title); putExtra(EXTRA_PATH, path)
        }
        context.sendBroadcast(intent)
        notification(context, title, context.getString(R.string.notif_download_complete), 100, complete = true)
    }

    fun broadcastFailed(context: Context, id: Long, title: String, error: String) {
        val intent = Intent(ACTION_FAILED).apply {
            putExtra(EXTRA_ID, id); putExtra(EXTRA_TITLE, title); putExtra(EXTRA_ERROR, error)
        }
        context.sendBroadcast(intent)
        notification(context, title, context.getString(R.string.notif_download_failed), 0, complete = false, error = error)
    }

    private fun notification(context: Context, title: String, text: String, percent: Int, complete: Boolean, error: String? = null) {
        val notif = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_DOWNLOADS)
            .setContentTitle(title)
            .setContentText(error ?: text)
            .setSmallIcon(if (complete) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_sys_download)
            .setOngoing(false)
            .setAutoCancel(true)
            .setProgress(100, percent, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            nm?.notify(2000, notif)
        } catch (_: SecurityException) {
            // missing notification permission on API 33+
        }
    }
}

/** Optional foreground service shell. Kept as a stub for future expansion (e.g. parallel workers). */
class DownloadServiceImpl : android.app.Service() {
    companion object {
        const val ACTION_CANCEL = "com.novatube.app.DOWNLOAD_CANCEL"
    }
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            val id = intent.getLongExtra(DownloadService.EXTRA_ID, -1L)
            if (id > 0) {
                com.novatube.app.download.DownloadScheduler.cancel(this, id)
                (applicationContext as? NovaTubeApp)?.downloadRepository?.markCancelled(id)
            }
        }
        return START_NOT_STICKY
    }
}
