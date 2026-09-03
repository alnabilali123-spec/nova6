@file:OptIn(ExperimentalMaterial3Api::class)

package com.novatube.app.ui.screens.music

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novatube.app.data.entity.DownloadEntity
import com.novatube.app.player.MusicPlaybackService
import com.novatube.app.util.AsyncImage
import com.novatube.app.viewmodel.LibraryViewModel
import com.novatube.app.viewmodel.ViewModelFactory

@Composable
fun MusicScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val vm: LibraryViewModel = viewModel(factory = ViewModelFactory.from(context))
    val audio by vm.audio.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
    ) {
        HeroArt(audio = audio)
        PlayerBar(audio = audio, onStart = { startIndex ->
            val intent = Intent(context, MusicPlaybackService::class.java)
            // The service creates its own queue in onCreate; we just start it so
            // ExoPlayer is ready. To start playback from a specific track we use
            // an action.
            intent.action = "com.novatube.app.action.PLAY_QUEUE"
            intent.putStringArrayListExtra(
                "tracks",
                ArrayList(audio.map { it.filePath ?: "" })
            )
            intent.putStringArrayListExtra(
                "titles",
                ArrayList(audio.map { it.title })
            )
            intent.putExtra("startIndex", startIndex)
            runCatching { context.startForegroundService(intent) }
        })
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Queue",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(audio) { item ->
                MusicRow(item = item, onPlay = { idx ->
                    val intent = Intent(context, MusicPlaybackService::class.java)
                    intent.action = "com.novatube.app.action.PLAY_QUEUE"
                    intent.putStringArrayListExtra("tracks", ArrayList(audio.map { it.filePath ?: "" }))
                    intent.putStringArrayListExtra("titles", ArrayList(audio.map { it.title }))
                    intent.putExtra("startIndex", idx)
                    runCatching { context.startForegroundService(intent) }
                })
            }
        }
    }
}

@Composable
private fun HeroArt(audio: List<DownloadEntity>) {
    val featured = audio.firstOrNull()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.primary
                    )
                )
            )
    ) {
        Column(modifier = Modifier.padding(20.dp).align(Alignment.BottomStart)) {
            Text(
                "Now Playing",
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                featured?.title ?: "Nothing in your library yet",
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (featured != null) {
                Text(
                    featured.uploader ?: "",
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PlayerBar(audio: List<DownloadEntity>, onStart: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (audio.isNotEmpty()) onStart(0) }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { /* prev */ }) { Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous") }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { /* next */ }) { Icon(Icons.Filled.SkipNext, contentDescription = "Next") }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { /* shuffle */ }) { Icon(Icons.Outlined.Shuffle, contentDescription = "Shuffle") }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { /* repeat */ }) { Icon(Icons.Outlined.Repeat, contentDescription = "Repeat") }
        }
    }
}

@Composable
private fun MusicRow(item: DownloadEntity, onPlay: (Int) -> Unit) {
    Card(
        onClick = { /* handled by parent via index */ },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                if (!item.thumbnail.isNullOrBlank()) {
                    AsyncImage(url = item.thumbnail, contentDescription = item.title, modifier = Modifier.fillMaxSize())
                }
                Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(item.uploader ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = { onPlay(0) }) { Icon(Icons.Filled.PlayArrow, contentDescription = "Play") }
        }
    }
}
