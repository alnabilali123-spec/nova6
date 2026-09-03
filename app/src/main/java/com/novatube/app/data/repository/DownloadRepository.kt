package com.novatube.app.data.repository

import android.content.Context
import com.novatube.app.data.dao.DownloadDao
import com.novatube.app.data.entity.DownloadEntity
import com.novatube.app.data.entity.DownloadStatus
import com.novatube.app.data.model.RequestedDownload
import com.novatube.app.util.FileUtils
import kotlinx.coroutines.flow.Flow
import java.io.File

class DownloadRepository(
    private val downloadDao: DownloadDao,
    private val context: Context
) {

    fun observeAll(): Flow<List<DownloadEntity>> = downloadDao.observeAll()
    fun observeActive(): Flow<List<DownloadEntity>> = downloadDao.observeActive()
    fun observeCompleted(): Flow<List<DownloadEntity>> = downloadDao.observeCompleted()
    fun observeFailed(): Flow<List<DownloadEntity>> = downloadDao.observeFailed()
    fun observeAudio(): Flow<List<DownloadEntity>> = downloadDao.observeAudioLibrary()
    fun observeVideo(): Flow<List<DownloadEntity>> = downloadDao.observeVideoLibrary()
    fun observeLibrary(): Flow<List<DownloadEntity>> = downloadDao.observeLibrary()
    fun observeActiveCount(): Flow<Int> = downloadDao.observeActiveCount()
    fun observeTotalSize(): Flow<Long?> = downloadDao.observeTotalSize()
    fun observeById(id: Long) = downloadDao.observeById(id)

    suspend fun get(id: Long): DownloadEntity? = downloadDao.getById(id)

    suspend fun enqueue(request: RequestedDownload): Long {
        val sanitized = FileUtils.sanitizeFileName(request.fileName)
        val ext = if (request.isAudioOnly) request.audioFormat else guessExt(request.formatId)
        val fullName = "$sanitized.$ext"
        val targetDir = if (request.isAudioOnly) FileUtils.audioDir(context) else FileUtils.downloadDir(context)
        val finalPath = "$targetDir/$fullName"

        val entity = DownloadEntity(
            url = request.url,
            webpageUrl = request.webpageUrl ?: request.url,
            title = request.title ?: request.fileName,
            uploader = request.uploader,
            thumbnail = request.thumbnail,
            duration = request.duration,
            formatId = request.formatId,
            ext = ext,
            isAudioOnly = request.isAudioOnly,
            audioFormat = if (request.isAudioOnly) request.audioFormat else null,
            fileName = fullName,
            filePath = finalPath,
            fileSize = 0,
            downloadedBytes = 0,
            speed = 0,
            eta = 0,
            progress = 0,
            status = DownloadStatus.QUEUED,
            embedSubtitles = request.embedSubtitles,
            subtitleLang = request.subtitleLang
        )
        return downloadDao.insert(entity)
    }

    suspend fun updateStatus(id: Long, status: DownloadStatus) = downloadDao.updateStatus(id, status)
    suspend fun markRunning(id: Long) = downloadDao.updateStatus(id, DownloadStatus.RUNNING)
    suspend fun markQueued(id: Long) = downloadDao.updateStatus(id, DownloadStatus.QUEUED)
    suspend fun markPaused(id: Long) = downloadDao.updateStatus(id, DownloadStatus.PAUSED)
    suspend fun markCancelled(id: Long) = downloadDao.updateStatus(id, DownloadStatus.CANCELLED)
    suspend fun markFailed(id: Long, error: String?) = downloadDao.markFailed(id, DownloadStatus.FAILED, error)
    suspend fun markCompleted(id: Long, path: String, size: Long) = downloadDao.markCompleted(id, DownloadStatus.COMPLETED, path, size)
    suspend fun updateProgress(id: Long, percent: Int, bytes: Long, speed: Long, eta: Long) =
        downloadDao.updateProgress(id, percent, bytes, speed, eta)
    suspend fun rename(id: Long, newName: String) {
        val entity = get(id) ?: return
        val old = entity.filePath?.let { File(it) } ?: return
        if (!old.exists()) return
        val newFile = File(old.parentFile, "$newName.${entity.ext}")
        if (old.renameTo(newFile)) {
            downloadDao.rename(id, newFile.name, newFile.absolutePath)
        }
    }
    suspend fun delete(id: Long) {
        get(id)?.filePath?.let { File(it).delete() }
        downloadDao.delete(id)
    }
    suspend fun clearCompleted() = downloadDao.clearCompleted()
    suspend fun clearAll() = downloadDao.clearAll()

    private fun guessExt(formatId: String): String {
        return when {
            formatId.contains("mp4", ignoreCase = true) -> "mp4"
            formatId.contains("webm", ignoreCase = true) -> "webm"
            formatId.contains("mkv", ignoreCase = true) -> "mkv"
            else -> "mp4"
        }
    }
}
