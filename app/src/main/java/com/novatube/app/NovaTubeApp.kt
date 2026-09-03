package com.novatube.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.novatube.app.core.YtDlpEngine
import com.novatube.app.data.db.AppDatabase
import com.novatube.app.data.prefs.PreferencesRepository
import com.novatube.app.data.repository.BookmarkRepository
import com.novatube.app.data.repository.DownloadRepository
import com.novatube.app.data.repository.HistoryRepository
import com.novatube.app.data.repository.PlaylistRepository
import com.novatube.app.data.repository.SearchHistoryRepository
import com.novatube.app.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NovaTubeApp : Application() {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(this) }
    val ytDlpEngine: YtDlpEngine by lazy { YtDlpEngine(this) }

    val downloadRepository: DownloadRepository by lazy { DownloadRepository(database.downloadDao(), this) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(database.historyDao()) }
    val bookmarkRepository: BookmarkRepository by lazy { BookmarkRepository(database.bookmarkDao()) }
    val searchHistoryRepository: SearchHistoryRepository by lazy { SearchHistoryRepository(database.searchHistoryDao()) }
    val playlistRepository: PlaylistRepository by lazy { PlaylistRepository(database.playlistDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        // Initialise the yt-dlp binary (extract from assets if needed).
        applicationScope.launch {
            ytDlpEngine.init()
            Log.i(TAG, "yt-dlp ready=${ytDlpEngine.state.value.ready} version=${ytDlpEngine.state.value.version} err=${ytDlpEngine.state.value.lastError}")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationHelper.CHANNEL_DOWNLOADS,
                getString(R.string.notif_channel_downloads),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationHelper.CHANNEL_PLAYER,
                getString(R.string.notif_channel_player),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationHelper.CHANNEL_MUSIC,
                getString(R.string.notif_channel_music),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationHelper.CHANNEL_GENERAL,
                getString(R.string.notif_channel_general),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    companion object {
        private const val TAG = "NovaTubeApp"
        @Volatile private var instance: NovaTubeApp? = null
        fun get(): NovaTubeApp = requireNotNull(instance) { "NovaTubeApp not initialised yet" }
    }
}
