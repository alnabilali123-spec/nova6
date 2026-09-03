package com.novatube.app.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.novatube.app.util.OkHttpProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A thin singleton wrapper around [ExoPlayer] that:
 *  - plays a single local file or a network URL
 *  - reuses one [OkHttpDataSource] for all media requests
 *  - exposes a [StateFlow] of the current playback state for Compose
 */
class VideoPlayerHolder private constructor(private val context: Context) {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val httpFactory: DataSource.Factory = OkHttpDataSource.Factory(OkHttpProvider.client)
        .setUserAgent("Mozilla/5.0 (Linux; Android 14) NovaTube/2.0")
    private val dataSourceFactory: DataSource.Factory =
        DefaultDataSource.Factory(context, httpFactory)

    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(buildMediaSourceFactory())
            .build()
            .also { p ->
                p.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        val s = _state.value
                        _state.value = s.copy(
                            isReady = state == Player.STATE_READY,
                            isBuffering = state == Player.STATE_BUFFERING,
                            ended = state == Player.STATE_ENDED,
                            errorCode = if (state == Player.STATE_IDLE) 1 else 0
                        )
                    }
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _state.value = _state.value.copy(isPlaying = isPlaying)
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        _state.value = _state.value.copy(errorCode = error.errorCode)
                    }
                })
            }
    }

    private fun buildMediaSourceFactory(): androidx.media3.exoplayer.source.MediaSource.Factory {
        return androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
    }

    fun playUrl(url: String, title: String? = null, headers: Map<String, String> = emptyMap(), mimeType: String? = null) {
        val itemBuilder = MediaItem.Builder()
            .setUri(url)
            .setMediaId(url)
        if (!title.isNullOrBlank()) itemBuilder.setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder().setTitle(title).build()
        )
        if (!mimeType.isNullOrBlank()) itemBuilder.setMimeType(mimeType)
        player.setMediaItem(itemBuilder.build())
        player.prepare()
        player.playWhenReady = true
        _state.value = PlayerState(isPlaying = true, currentMediaUrl = url, title = title)
    }

    fun playLocalFile(path: String, title: String? = null) {
        playUrl("file://$path", title = title, mimeType = MimeTypes.VIDEO_MP4)
    }

    fun togglePlay() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun seekDelta(deltaMs: Long) = player.seekTo((player.currentPosition + deltaMs).coerceAtLeast(0))

    fun setSpeed(speed: Float) {
        player.playbackParameters = player.playbackParameters.withSpeed(speed.coerceIn(0.25f, 3.0f))
    }

    fun release() {
        runCatching { player.release() }
    }

    data class PlayerState(
        val isPlaying: Boolean = false,
        val isReady: Boolean = false,
        val isBuffering: Boolean = false,
        val ended: Boolean = false,
        val errorCode: Int = 0,
        val currentMediaUrl: String? = null,
        val title: String? = null
    )

    companion object {
        @Volatile private var INSTANCE: VideoPlayerHolder? = null
        fun get(context: Context): VideoPlayerHolder = INSTANCE ?: synchronized(this) {
            INSTANCE ?: VideoPlayerHolder(context.applicationContext).also { INSTANCE = it }
        }
    }
}
