package com.novatube.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.novatube.app.data.dao.BookmarkDao
import com.novatube.app.data.dao.DownloadDao
import com.novatube.app.data.dao.HistoryDao
import com.novatube.app.data.dao.PlaylistDao
import com.novatube.app.data.dao.SearchHistoryDao
import com.novatube.app.data.entity.BookmarkEntity
import com.novatube.app.data.entity.DownloadEntity
import com.novatube.app.data.entity.DownloadStatus
import com.novatube.app.data.entity.HistoryEntity
import com.novatube.app.data.entity.PlaylistEntity
import com.novatube.app.data.entity.PlaylistTrack
import com.novatube.app.data.entity.SearchHistoryEntity

class Converters {
    @TypeConverter
    fun fromStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): DownloadStatus = runCatching { DownloadStatus.valueOf(value) }.getOrDefault(DownloadStatus.QUEUED)
}

@Database(
    entities = [
        DownloadEntity::class,
        HistoryEntity::class,
        BookmarkEntity::class,
        SearchHistoryEntity::class,
        PlaylistEntity::class,
        PlaylistTrack::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "novatube.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
