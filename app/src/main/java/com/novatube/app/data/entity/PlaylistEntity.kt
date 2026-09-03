package com.novatube.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlists",
    indices = [Index("createdAt")]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val coverArt: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val trackCount: Int = 0
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "downloadId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DownloadEntity::class,
            parentColumns = ["id"],
            childColumns = ["downloadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("downloadId"), Index("position")]
)
data class PlaylistTrack(
    val playlistId: Long,
    val downloadId: Long,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history", indices = [Index("query", unique = true), Index("searchedAt")])
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)
