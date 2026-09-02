@file:OptIn(ExperimentalMaterial3Api::class)

package com.novatube.app.ui.screens.downloads

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novatube.app.R
import com.novatube.app.data.entity.DownloadEntity
import com.novatube.app.ui.components.DownloadCard
import com.novatube.app.ui.components.LinearProgress
import com.novatube.app.util.UrlUtils
import com.novatube.app.viewmodel.DownloadsViewModel
import com.novatube.app.viewmodel.ViewModelFactory
import java.io.File

@Composable
fun DownloadsScreen(paddingValues: PaddingValues, onOpenLocal: (path: String, title: String) -> Unit) {
    val context = LocalContext.current
    val vm: DownloadsViewModel = viewModel(factory = ViewModelFactory.from(context))
    val active by vm.active.collectAsStateWithLifecycle()
    val completed by vm.completed.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    val batch by vm.batchUrls.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) }
    var batchDialog by remember { mutableStateOf(false) }
    val tabs = listOf(
        stringResource(R.string.downloads_active) to active,
        stringResource(R.string.downloads_completed) to completed,
        stringResource(R.string.downloads_failed) to failed
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.downloads_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { batchDialog = true }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Batch") }
            IconButton(onClick = { vm.clearCompleted() }) { Icon(Icons.Outlined.Delete, contentDescription = "Clear") }
        }
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { i, (label, items) ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = { Text("$label (${items.size})") }
                )
            }
        }
        val (_, list) = tabs[tab]
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.downloads_empty), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.downloads_empty_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list) { entity ->
                    DownloadRow(
                        entity = entity,
                        onOpen = { onOpenLocal(entity.filePath ?: "", entity.title) },
                        onRetry = { vm.retry(entity) },
                        onCancel = { vm.cancel(entity) },
                        onDelete = { vm.delete(entity) },
                        onShare = { shareFile(context, entity) }
                    )
                }
            }
        }
    }

    if (batchDialog) {
        BatchDialog(
            initial = batch,
            onDismiss = { batchDialog = false },
            onSubmit = { urls ->
                vm.enqueueBatch(urls)
                batchDialog = false
            }
        )
    }
}

@Composable
private fun DownloadRow(
    entity: DownloadEntity,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            DownloadCard(
                entity = entity,
                onClick = onOpen,
                onAction = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val sizeText = if (entity.fileSize > 0) {
                    com.novatube.app.util.FileUtils.humanReadableSize(entity.fileSize)
                } else "—"
                val speedText = if (entity.status == com.novatube.app.data.entity.DownloadStatus.RUNNING && entity.speed > 0)
                    com.novatube.app.util.formatSpeed(entity.speed) else null
                Text(
                    text = buildString {
                        append(sizeText)
                        if (speedText != null) append(" • $speedText")
                        if (entity.eta > 0 && entity.status == com.novatube.app.data.entity.DownloadStatus.RUNNING) {
                            append(" • ETA ${com.novatube.app.util.formatEta(entity.eta)}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                when (entity.status) {
                    com.novatube.app.data.entity.DownloadStatus.FAILED -> {
                        IconButton(onClick = onRetry) { Icon(Icons.Outlined.Refresh, contentDescription = "Retry") }
                    }
                    com.novatube.app.data.entity.DownloadStatus.RUNNING,
                    com.novatube.app.data.entity.DownloadStatus.QUEUED -> {
                        IconButton(onClick = onCancel) { Icon(Icons.Outlined.Cancel, contentDescription = "Cancel") }
                    }
                    com.novatube.app.data.entity.DownloadStatus.COMPLETED -> {
                        IconButton(onClick = onShare) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Share") }
                    }
                    else -> {}
                }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete") }
            }
            if (entity.status == com.novatube.app.data.entity.DownloadStatus.RUNNING ||
                entity.status == com.novatube.app.data.entity.DownloadStatus.QUEUED) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgress(value = entity.progress / 100f, height = 4)
            }
            entity.errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun shareFile(context: android.content.Context, entity: DownloadEntity) {
    val path = entity.filePath ?: return
    val file = File(path)
    if (!file.exists()) return
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull() ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, entity.title))
}

@Composable
private fun BatchDialog(
    initial: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (List<String>) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf(TextFieldValue(initial.joinToString("\n"))) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.downloads_batch_paste)) },
        text = {
            Column {
                Text(stringResource(R.string.downloads_paste_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    placeholder = { Text("https://…") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = {
                    val pasted = clipboard.getText()?.text ?: ""
                    if (pasted.isNotBlank()) text = TextFieldValue(UrlUtils.allUrls(pasted).joinToString("\n"))
                }) { Text(stringResource(R.string.common_refresh)) }
            }
        },
        confirmButton = {
            val count = text.text.lines().count { it.trim().isNotBlank() }
            TextButton(onClick = { onSubmit(text.text.lines().map { it.trim() }.filter { it.isNotBlank() }) }) {
                Text(stringResource(R.string.downloads_start_batch, count))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}
