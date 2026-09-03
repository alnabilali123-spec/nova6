package com.novatube.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DownloadStatus { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED }

@Entity(
    tableName = "downloads",
    indices = [Index("status"), Index("createdAt"), Index("isAudioOnly")]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val webpageUrl: String?,
    val title: String,
    val uploader: String?,
    val thumbnail: String?,
    val duration: Long?,
    val formatId: String,
    val ext: String,
    val isAudioOnly: Boolean,
    val audioFormat: String?,
    val fileName: String,
    val filePath: String?,
    val fileSize: Long,
    val downloadedBytes: Long,
    val speed: Long,
    val eta: Long,
    val progress: Int,
    val status: DownloadStatus,
    val errorMessage: String? = null,
    val embedSubtitles: Boolean = false,
    val subtitleLang: String = "en,ar",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
