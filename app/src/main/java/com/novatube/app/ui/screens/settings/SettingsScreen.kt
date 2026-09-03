@file:OptIn(ExperimentalMaterial3Api::class)

package com.novatube.app.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novatube.app.BuildConfig
import com.novatube.app.R
import com.novatube.app.data.prefs.AudioFormat
import com.novatube.app.data.prefs.ThemeMode
import com.novatube.app.data.prefs.VideoQuality
import com.novatube.app.util.FileUtils
import com.novatube.app.viewmodel.SettingsViewModel
import com.novatube.app.viewmodel.ViewModelFactory

@Composable
fun SettingsScreen(paddingValues: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel(factory = ViewModelFactory.from(context))
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val engine by vm.engineState.collectAsStateWithLifecycle()

    var confirmClearDownloads by remember { mutableStateOf(false) }
    var confirmClearSearch by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }

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
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionCard(title = stringResource(R.string.settings_appearance), icon = Icons.Outlined.ColorLens) {
                EnumRow(
                    label = stringResource(R.string.settings_theme),
                    current = prefs.themeMode.name,
                    options = ThemeMode.values().map { it.name to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onSelect = { vm.setTheme(ThemeMode.valueOf(it)) }
                )
                EnumRow(
                    label = stringResource(R.string.settings_language),
                    current = prefs.language,
                    options = listOf("system" to "System", "en" to "English", "ar" to "العربية"),
                    onSelect = { vm.setLanguage(it) }
                )
            } }

            item { SectionCard(title = stringResource(R.string.settings_downloads), icon = Icons.Outlined.Download) {
                EnumRow(
                    label = stringResource(R.string.settings_quality),
                    current = prefs.videoQuality.name,
                    options = VideoQuality.values().map { it.name to prettifyQuality(it) },
                    onSelect = { vm.setVideoQuality(VideoQuality.valueOf(it)) }
                )
                EnumRow(
                    label = stringResource(R.string.settings_audio_format),
                    current = prefs.audioFormat.name,
                    options = AudioFormat.values().map { it.name to it.name },
                    onSelect = { vm.setAudioFormat(AudioFormat.valueOf(it)) }
                )
                SwitchRow(
                    label = stringResource(R.string.settings_wifi_only),
                    checked = prefs.wifiOnly,
                    onChange = { vm.setWifiOnly(it) }
                )
                SwitchRow(
                    label = stringResource(R.string.settings_clipboard),
                    checked = prefs.clipboardDetection,
                    onChange = { vm.setClipboard(it) }
                )
                SwitchRow(
                    label = "Embed metadata",
                    checked = prefs.embedMetadata,
                    onChange = { vm.setEmbedMetadata(it) }
                )
                SwitchRow(
                    label = stringResource(R.string.player_subtitles),
                    checked = prefs.embedSubtitles,
                    onChange = { vm.setEmbedSubtitles(it) }
                )
                EnumRow(
                    label = "Subtitle languages",
                    current = prefs.subtitleLang,
                    options = listOf("en" to "English", "ar" to "Arabic", "en,ar" to "English + Arabic", "en,ar,fr,es" to "Multi (4)"),
                    onSelect = { vm.setSubtitleLang(it) }
                )
                IntSliderRow(
                    label = stringResource(R.string.settings_max_parallel),
                    value = prefs.maxParallelDownloads,
                    range = 1..8,
                    onChange = { vm.setMaxParallel(it) }
                )
            } }

            item { SectionCard(title = stringResource(R.string.settings_storage), icon = Icons.Outlined.Storage) {
                val audioDir = FileUtils.audioRoot(context)
                val videoDir = FileUtils.downloadsRoot(context)
                InfoRow(label = stringResource(R.string.library_audio), value = audioDir.absolutePath)
                InfoRow(label = stringResource(R.string.library_video), value = videoDir.absolutePath)
            } }

            item { SectionCard(title = stringResource(R.string.settings_general), icon = Icons.Outlined.Build) {
                SwitchRow(
                    label = stringResource(R.string.settings_notifications),
                    checked = prefs.notificationsEnabled,
                    onChange = { vm.setNotifications(it) }
                )
                SwitchRow(
                    label = "Browser JavaScript",
                    checked = prefs.javascriptEnabled,
                    onChange = { /* persisted in browser VM */ }
                )
                SwitchRow(
                    label = "Browser desktop mode",
                    checked = prefs.desktopMode,
                    onChange = { /* persisted in browser VM */ }
                )
                ActionRow(
                    label = stringResource(R.string.settings_clear_search),
                    icon = Icons.Outlined.ClearAll,
                    onClick = { confirmClearSearch = true }
                )
                ActionRow(
                    label = stringResource(R.string.settings_clear_history),
                    icon = Icons.Outlined.ClearAll,
                    onClick = { confirmClearHistory = true }
                )
                ActionRow(
                    label = stringResource(R.string.settings_clear_downloads),
                    icon = Icons.Outlined.ClearAll,
                    onClick = { confirmClearDownloads = true }
                )
            } }

            item { SectionCard(title = stringResource(R.string.settings_about), icon = Icons.Outlined.Info) {
                InfoRow(label = "Version", value = BuildConfig.VERSION_NAME)
                InfoRow(label = "yt-dlp", value = engine.version ?: "Not ready")
                InfoRow(
                    label = "App version code",
                    value = BuildConfig.VERSION_CODE.toString()
                )
                Text(
                    text = stringResource(R.string.settings_credits),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } }
        }
    }

    if (confirmClearDownloads) ConfirmDialog(
        text = "Clear all completed downloads?",
        onConfirm = { confirmClearDownloads = false; vm.clearAllDownloads() },
        onDismiss = { confirmClearDownloads = false }
    )
    if (confirmClearSearch) ConfirmDialog(
        text = "Clear search history?",
        onConfirm = { confirmClearSearch = false; vm.clearSearchHistory() },
        onDismiss = { confirmClearSearch = false }
    )
    if (confirmClearHistory) ConfirmDialog(
        text = "Clear browser history?",
        onConfirm = { confirmClearHistory = false; vm.clearBrowserHistory() },
        onDismiss = { confirmClearHistory = false }
    )
}

private fun prettifyQuality(q: VideoQuality): String = when (q) {
    VideoQuality.BEST -> "Best available"
    VideoQuality.HIGH_1080 -> "1080p (Full HD)"
    VideoQuality.MEDIUM_720 -> "720p (HD)"
    VideoQuality.LOW_480 -> "480p"
    VideoQuality.AUDIO_ONLY -> "Audio only (mp3)"
}

@Composable
private fun SectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun EnumRow(
    label: String,
    current: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == current }?.second ?: current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.TextButton(onClick = { expanded = true }) {
            Text(currentLabel, color = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, display) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = { expanded = false; onSelect(value) }
                )
            }
        }
    }
}

@Composable
private fun IntSliderRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(value.toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt().coerceIn(range)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActionRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = onClick) { Text(stringResource(R.string.common_apply)) }
    }
}

@Composable
private fun ConfirmDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.common_delete)) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.common_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}
