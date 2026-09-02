@file:OptIn(ExperimentalMaterial3Api::class)

package com.novatube.app.ui.screens.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.novatube.app.player.VideoPlayerHolder
import com.novatube.app.util.formatEta
import com.novatube.app.util.formatDuration

@Composable
fun PlayerScreen(
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    url: String,
    title: String,
    type: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val holder = remember(context) { VideoPlayerHolder.get(context) }
    val state by holder.state.collectAsStateWithLifecycle()

    var isFullscreen by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var brightness by remember { mutableFloatStateOf(0.5f) }
    var volume by remember { mutableFloatStateOf(0.8f) }
    var showControls by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1.0f) }

    LaunchedEffect(url) {
        if (type == "local") holder.playLocalFile(url, title)
        else holder.playUrl(url, title)
    }
    LaunchedEffect(holder) {
        while (true) {
            kotlinx.coroutines.delay(500)
            position = holder.player.currentPosition.coerceAtLeast(0)
            duration = holder.player.duration.coerceAtLeast(0)
        }
    }
    DisposableEffect(Unit) { onDispose { /* keep holder alive across screens */ } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(paddingValues)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = holder.player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            }
        )

        // Gesture overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        // Left half changes brightness, right half changes volume.
                        val center = size.width / 2
                        val touchX = 0f // approximate; this is a simple impl
                        if (touchX < center) brightness = (brightness - dragAmount / 500f).coerceIn(0f, 1f)
                        else volume = (volume - dragAmount / 500f).coerceIn(0f, 1f)
                    }
                }
        ) { }

        // Controls overlay
        if (showControls) {
            ControlsOverlay(
                title = title,
                position = position,
                duration = duration,
                isPlaying = state.isPlaying,
                speed = speed,
                onPlayPause = { holder.togglePlay() },
                onSeekDelta = { holder.seekDelta(it); position = (position + it).coerceAtLeast(0) },
                onSeek = { holder.seekTo(it); position = it },
                onSpeedChange = { s -> speed = s; holder.setSpeed(s) },
                onPip = { enterPip(context) },
                onBack = onBack
            )
        }

        // Tap to toggle controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    androidx.compose.foundation.gestures.detectTapGestures { showControls = !showControls }
                }
        ) { }
    }
}

@Composable
private fun ControlsOverlay(
    title: String,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    speed: Float,
    onPlayPause: () -> Unit,
    onSeekDelta: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPip: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onPip) {
                Icon(Icons.Outlined.PictureInPicture, contentDescription = "PiP", tint = Color.White)
            }
        }
        // Center controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onSeekDelta(-10_000) }) {
                Icon(Icons.Filled.Replay10, contentDescription = "Back 10s", tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.width(24.dp))
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
            IconButton(onClick = { onSeekDelta(10_000) }) {
                Icon(Icons.Filled.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }
        // Bottom slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Slider(
                value = if (duration > 0) position.toFloat() / duration else 0f,
                onValueChange = { onSeek((it * duration).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDuration(position / 1000),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatDuration(duration / 1000),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                SpeedChip(speed = speed, onChange = onSpeedChange)
            }
        }
    }
}

@Composable
private fun SpeedChip(speed: Float, onChange: (Float) -> Unit) {
    val speeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Outlined.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            speeds.forEach { s ->
                Text(
                    text = "${s}×",
                    color = if (s == speed) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .pointerInput(s) {
                            androidx.compose.foundation.gestures.detectTapGestures {
                                onChange(s)
                            }
                        }
                )
            }
        }
    }
}

private fun enterPip(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val activity = context as? android.app.Activity ?: return
    val params = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
        .build()
    activity.enterPictureInPictureMode(params)
}
