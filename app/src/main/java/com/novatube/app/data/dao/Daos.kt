package com.novatube.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novatube.app.data.entity.BookmarkEntity
import com.novatube.app.data.entity.HistoryEntity
import com.novatube.app.data.entity.PlaylistEntity
import com.novatube.app.data.entity.PlaylistTrack
import com.novatube.app.data.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity): Long

    @Query("SELECT * FROM history ORDER BY viewedAt DESC LIMIT 200")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY viewedAt DESC LIMIT 20")
    fun observeRecent(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: BookmarkEntity): Long

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): BookmarkEntity?

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM bookmarks")
    suspend fun clear()
}

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchHistoryEntity): Long

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 50")
    fun observeAll(): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteByQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clear()
}

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: Long): PlaylistEntity?

    @Query("UPDATE playlists SET name = :name, description = :description, updatedAt = :now WHERE id = :id")
    suspend fun updatePlaylist(id: Long, name: String, description: String?, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrack(track: PlaylistTrack)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeTracks(playlistId: Long): Flow<List<PlaylistTrack>>

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND downloadId = :downloadId")
    suspend fun removeTrack(playlistId: Long, downloadId: Long)

    @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :playlistId AND downloadId = :downloadId")
    suspend fun updateTrackPosition(playlistId: Long, downloadId: Long, position: Int)

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun trackCount(playlistId: Long): Int

    @Query("SELECT MAX(position) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int?
}
