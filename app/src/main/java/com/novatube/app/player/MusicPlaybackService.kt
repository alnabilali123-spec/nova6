package com.novatube.app.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.novatube.app.MainActivity
import com.novatube.app.util.OkHttpProvider

/**
 * Foreground MediaSession service for the in-app audio player. Handles a
 * `com.novatube.app.action.PLAY_QUEUE` intent whose extras are:
 *  - `tracks`:   ArrayList<String>  — local file paths
 *  - `titles`:   ArrayList<String>  — display titles aligned with tracks
 *  - `startIndex`: Int               — index to start from
 */
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        val httpFactory = OkHttpDataSource.Factory(OkHttpProvider.client)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) NovaTube/2.0")
        val ds = DefaultDataSource.Factory(this, httpFactory)
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val sessionActivity = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PLAY_QUEUE) {
            val tracks = intent.getStringArrayListExtra(EXTRA_TRACKS) ?: arrayListOf()
            val titles = intent.getStringArrayListExtra(EXTRA_TITLES) ?: arrayListOf()
            val startIndex = intent.getIntExtra(EXTRA_START, 0).coerceIn(0, (tracks.size - 1).coerceAtLeast(0))
            if (tracks.isNotEmpty()) {
                val items = tracks.mapIndexed { i, path ->
                    MediaItem.Builder()
                        .setUri(if (path.startsWith("/")) "file://$path" else path)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(titles.getOrNull(i) ?: "Track $i")
                                .setArtist("NovaTube")
                                .build()
                        )
                        .build()
                }
                player.setMediaItems(items, startIndex, 0L)
                player.prepare()
                player.playWhenReady = true
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY_QUEUE = "com.novatube.app.action.PLAY_QUEUE"
        const val EXTRA_TRACKS = "tracks"
        const val EXTRA_TITLES = "titles"
        const val EXTRA_START = "startIndex"
    }
}
