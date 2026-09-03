package com.novatube.app.data.repository

import com.novatube.app.data.dao.BookmarkDao
import com.novatube.app.data.dao.HistoryDao
import com.novatube.app.data.dao.PlaylistDao
import com.novatube.app.data.dao.SearchHistoryDao
import com.novatube.app.data.entity.BookmarkEntity
import com.novatube.app.data.entity.HistoryEntity
import com.novatube.app.data.entity.PlaylistEntity
import com.novatube.app.data.entity.PlaylistTrack
import com.novatube.app.data.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: HistoryDao) {
    fun observeAll(): Flow<List<HistoryEntity>> = dao.observeAll()
    fun observeRecent(): Flow<List<HistoryEntity>> = dao.observeRecent()
    suspend fun add(title: String, url: String, thumbnail: String?, uploader: String?, duration: Long?, platform: String?) =
        dao.insert(HistoryEntity(title = title, url = url, thumbnail = thumbnail, uploader = uploader, duration = duration, platform = platform))
    suspend fun clear() = dao.clear()
}

class BookmarkRepository(private val dao: BookmarkDao) {
    fun observeAll(): Flow<List<BookmarkEntity>> = dao.observeAll()
    suspend fun add(title: String, url: String, favicon: String? = null, folder: String? = null) =
        dao.insert(BookmarkEntity(title = title, url = url, favicon = favicon, folder = folder))
    suspend fun remove(id: Long) = dao.delete(id)
    suspend fun clear() = dao.clear()
    suspend fun find(url: String) = dao.findByUrl(url)
}

class SearchHistoryRepository(private val dao: SearchHistoryDao) {
    fun observeAll(): Flow<List<SearchHistoryEntity>> = dao.observeAll()
    suspend fun add(query: String) = dao.insert(SearchHistoryEntity(query = query))
    suspend fun remove(query: String) = dao.deleteByQuery(query)
    suspend fun clear() = dao.clear()
}

class PlaylistRepository(private val dao: PlaylistDao) {
    fun observeAll(): Flow<List<PlaylistEntity>> = dao.observePlaylists()
    fun observeTracks(playlistId: Long): Flow<List<PlaylistTrack>> = dao.observeTracks(playlistId)
    suspend fun get(id: Long): PlaylistEntity? = dao.getPlaylist(id)
    suspend fun create(name: String, description: String? = null): Long =
        dao.insertPlaylist(PlaylistEntity(name = name, description = description))
    suspend fun update(id: Long, name: String, description: String?) =
        dao.updatePlaylist(id, name, description, System.currentTimeMillis())
    suspend fun delete(id: Long) = dao.deletePlaylist(id)
    suspend fun addTrack(playlistId: Long, downloadId: Long) {
        val next = (dao.maxPosition(playlistId) ?: -1) + 1
        dao.addTrack(PlaylistTrack(playlistId, downloadId, next))
    }
    suspend fun removeTrack(playlistId: Long, downloadId: Long) =
        dao.removeTrack(playlistId, downloadId)
}
