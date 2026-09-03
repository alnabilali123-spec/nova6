@file:OptIn(ExperimentalMaterial3Api::class)

package com.novatube.app.ui.screens.format

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novatube.app.R
import com.novatube.app.data.model.MediaFormat
import com.novatube.app.data.prefs.AudioFormat
import com.novatube.app.ui.components.LinearProgress
import com.novatube.app.util.AsyncImage
import com.novatube.app.viewmodel.FormatSelectionViewModel
import com.novatube.app.viewmodel.ViewModelFactory

@Composable
fun FormatSelectionScreen(
    paddingValues: PaddingValues,
    url: String,
    onBack: () -> Unit,
    onEnqueued: () -> Unit
) {
    val context = LocalContext.current
    val vm: FormatSelectionViewModel = viewModel(factory = ViewModelFactory.from(context))
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(url) { vm.load(url) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
            Text(
                text = stringResource(R.string.format_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.format_extracting), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (state.info == null) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.format_error), color = MaterialTheme.colorScheme.error)
            }
        } else {
            val info = state.info!!
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    MediaHeader(
                        title = info.title ?: "—",
                        uploader = info.uploader,
                        duration = info.duration,
                        thumbnail = info.thumbnail
                    )
                }
                item {
                    QuickActions(
                        onBestVideo = { vm.quickBestVideo { onEnqueued() } },
                        onBestAudio = { vm.quickBestAudio { onEnqueued() } }
                    )
                }
                item {
                    Text(
                        stringResource(R.string.format_video),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                if (state.videoFormats.isEmpty()) {
                    item { Text(stringResource(R.string.format_no_video), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(state.videoFormats) { fmt ->
                        FormatRow(format = fmt, isAudio = false) { vm.enqueue(fmt, isAudio = false) { onEnqueued() } }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.format_audio),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                item {
                    AudioFormatPicker(current = state.selectedAudio) { vm.setAudioFormat(it) }
                }
                if (state.audioFormats.isEmpty()) {
                    item { Text(stringResource(R.string.format_no_audio), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(state.audioFormats) { fmt ->
                        FormatRow(format = fmt, isAudio = true) { vm.enqueue(fmt, isAudio = true) { onEnqueued() } }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaHeader(title: String, uploader: String?, duration: Long?, thumbnail: String?) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!thumbnail.isNullOrBlank()) {
                    AsyncImage(url = thumbnail, contentDescription = title, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                if (!uploader.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        uploader,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (duration != null && duration > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        com.novatube.app.util.formatDuration(duration),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActions(onBestVideo: () -> Unit, onBestAudio: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onBestVideo,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Outlined.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.format_best_video))
        }
        Button(
            onClick = onBestAudio,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(Icons.Outlined.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.format_best_audio))
        }
    }
}

@Composable
private fun AudioFormatPicker(current: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(AudioFormat.values().toList()) { fmt ->
            val name = when (fmt) {
                AudioFormat.MP3 -> "MP3"
                AudioFormat.M4A -> "M4A / AAC"
                AudioFormat.OPUS -> "OPUS"
                AudioFormat.WAV -> "WAV"
                AudioFormat.FLAC -> "FLAC"
            }
            FilterChip(
                selected = current.equals(fmt.name, ignoreCase = true),
                onClick = { onSelect(fmt.name.lowercase()) },
                label = { Text(name) }
            )
        }
    }
}

@Composable
private fun FormatRow(format: MediaFormat, isAudio: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isAudio) Icons.Outlined.GraphicEq else Icons.Outlined.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAudio) {
                        "Audio • ${format.displayBitrate}"
                    } else {
                        format.displayResolution
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = format.displayExt + " • " + format.formatNote.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            format.displaySize?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
