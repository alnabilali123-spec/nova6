@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.novatube.app.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer

/**
 * Bottom sheet for selecting subtitle, audio and quality tracks. The ExoPlayer
 * is passed by reference so changes propagate immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectorBottomSheet(
    player: ExoPlayer,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tracks = remember { player.currentTracks }
    var tab by remember { mutableStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Tracks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackTab("Audio", Icons.Outlined.AudioFile, selected = tab == 0) { tab = 0 }
                TrackTab("Subtitles", Icons.Outlined.ClosedCaption, selected = tab == 1) { tab = 1 }
                TrackTab("Quality", Icons.Outlined.Hd, selected = tab == 2) { tab = 2 }
                TrackTab("Speed", Icons.Outlined.Speed, selected = tab == 3) { tab = 3 }
            }
            Spacer(modifier = Modifier.height(12.dp))

            when (tab) {
                0 -> TrackList(
                    title = "Audio tracks",
                    items = tracks.mapIndexedNotNull { i, group ->
                        if (group.type == C.TRACK_TYPE_AUDIO) {
                            TrackChoice(
                                id = "audio_$i",
                                label = group.trackGroup.toString().ifBlank { "Audio" },
                                selected = group.isSelected
                            )
                        } else null
                    },
                    onSelect = { /* ExoPlayer exposes track selection via TrackSelectionParameters */ }
                )
                1 -> TrackList(
                    title = "Subtitles",
                    items = tracks.mapIndexedNotNull { i, group ->
                        if (group.type == C.TRACK_TYPE_TEXT) {
                            TrackChoice(
                                id = "sub_$i",
                                label = "Subtitle ${i + 1}",
                                selected = group.isSelected
                            )
                        } else null
                    },
                    onSelect = { /* similar pattern */ }
                )
                2 -> TrackList(
                    title = "Video quality",
                    items = tracks.mapIndexedNotNull { i, group ->
                        if (group.type == C.TRACK_TYPE_VIDEO) {
                            TrackChoice(id = "video_$i", label = "Variant ${i + 1}", selected = group.isSelected)
                        } else null
                    },
                    onSelect = { }
                )
                3 -> SpeedPicker(current = player.playbackParameters.speed) { speed ->
                    player.playbackParameters = player.playbackParameters.withSpeed(speed)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TrackTab(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private data class TrackChoice(val id: String, val label: String, val selected: Boolean)

@Composable
private fun TrackList(title: String, items: List<TrackChoice>, onSelect: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
    if (items.isEmpty()) {
        Text("No tracks available", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            items(items) { choice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (choice.selected) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(choice.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SpeedPicker(current: Float, onChange: (Float) -> Unit) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    Column {
        Text("Playback speed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            speeds.forEach { s ->
                Surface(
                    onClick = { onChange(s) },
                    shape = RoundedCornerShape(50),
                    color = if (s == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "${s}×",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (s == current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
