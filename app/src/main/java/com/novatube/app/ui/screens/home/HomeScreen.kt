@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.novatube.app.ui.screens.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novatube.app.R
import com.novatube.app.data.model.SearchResult
import com.novatube.app.ui.components.CompactMediaRow
import com.novatube.app.ui.components.MediaCard
import com.novatube.app.util.AsyncImage
import com.novatube.app.util.UrlUtils
import com.novatube.app.viewmodel.HomeViewModel
import com.novatube.app.viewmodel.ViewModelFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToFormat: (String) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMusic: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPlay: (path: String, title: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = ViewModelFactory.from(context))
    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val totalSize by viewModel.totalSize.collectAsStateWithLifecycle()
    val engineReady = viewModel.engineState.collectAsStateWithLifecycle().value.ready

    var url by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            HeroHeader(activeCount = activeCount, totalSizeBytes = totalSize, engineReady = engineReady)
            QuickDownloadCard(
                url = url,
                onUrlChange = { url = it },
                onPaste = {
                    val text = clipboard.getText()?.text
                    if (!text.isNullOrBlank()) {
                        val first = UrlUtils.extractFirstUrl(text)
                        if (first != null) url = first else url = text
                    }
                },
                onDetectLinks = {
                    val text = clipboard.getText()?.text
                    UrlUtils.allUrls(text)
                },
                onSubmit = { entered ->
                    val finalUrl = if (entered.startsWith("http", ignoreCase = true)) entered
                    else "https://www.google.com/search?q=${URLEncoder.encode(entered, StandardCharsets.UTF_8.name())}"
                    onNavigateToFormat(finalUrl)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            QuickActions(
                onSearch = { onNavigateToSearch(null) },
                onBrowser = onNavigateToBrowser,
                onLibrary = onNavigateToLibrary,
                onHistory = onNavigateToHistory,
                onMusic = onNavigateToMusic,
                onDownloads = onNavigateToDownloads
            )
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(text = stringResource(R.string.home_trending), icon = Icons.Outlined.Whatshot)
            TrendingCarousel(onItemClick = onNavigateToFormat)
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(text = stringResource(R.string.home_recent_downloads), icon = Icons.Outlined.History)
            if (recent.isEmpty()) {
                EmptyHint(text = stringResource(R.string.home_no_recent))
            } else {
                recent.take(6).forEach { entity ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        CompactMediaRow(
                            title = entity.title,
                            subtitle = entity.uploader,
                            thumbnail = entity.thumbnail,
                            onClick = {
                                val path = entity.filePath
                                if (path != null) onPlay(path, entity.title)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(text = stringResource(R.string.home_discover), icon = Icons.Outlined.Public)
            PlatformsGrid(onClick = onNavigateToFormat)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroHeader(activeCount: Int, totalSizeBytes: Long?, engineReady: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatPill(icon = Icons.Outlined.Download, label = "$activeCount active")
                Spacer(modifier = Modifier.width(8.dp))
                StatPill(
                    icon = Icons.Outlined.Storage,
                    label = totalSizeBytes?.let { com.novatube.app.util.FileUtils.humanReadableSize(it) } ?: "0 KB"
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatPill(
                    icon = Icons.Outlined.Speed,
                    label = if (engineReady) "yt-dlp ready" else "yt-dlp…"
                )
            }
        }
    }
}

@Composable
private fun StatPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = Color.White.copy(alpha = 0.18f),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun QuickDownloadCard(
    url: String,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onDetectLinks: () -> List<String>,
    onSubmit: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_quick_download),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_paste_url),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                placeholder = { Text(stringResource(R.string.home_url_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onPaste) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste")
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.Button(
                onClick = { onSubmit(url) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.common_apply))
            }
        }
    }
}

@Composable
private fun QuickActions(
    onSearch: () -> Unit,
    onBrowser: () -> Unit,
    onLibrary: () -> Unit,
    onHistory: () -> Unit,
    onMusic: () -> Unit,
    onDownloads: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionTile(icon = Icons.Outlined.Search, label = stringResource(R.string.nav_search), onClick = onSearch)
        ActionTile(icon = Icons.Outlined.Public, label = stringResource(R.string.nav_browser), onClick = onBrowser)
        ActionTile(icon = Icons.Outlined.LibraryBooks, label = stringResource(R.string.nav_library), onClick = onLibrary)
        ActionTile(icon = Icons.Outlined.MusicNote, label = stringResource(R.string.nav_music), onClick = onMusic)
        ActionTile(icon = Icons.Outlined.Download, label = stringResource(R.string.nav_downloads), onClick = onDownloads)
    }
}

@Composable
private fun ActionTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun SectionHeader(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TrendingCarousel(onItemClick: (String) -> Unit) {
    val items = remember {
        listOf(
            SearchResult("yt-1", "Top Trending Music Videos 2026", "Mix", 3845L, null, "https://www.youtube.com/results?search_query=trending+music+2026", com.novatube.app.data.model.SearchKind.VIDEO, "YouTube"),
            SearchResult("yt-2", "Best of 2026 Playlists", "Spotify", 7210L, null, "https://soundcloud.com/", com.novatube.app.data.model.SearchKind.AUDIO, "SoundCloud"),
            SearchResult("yt-3", "Viral TikTok Compilations", "Daily", 2400L, null, "https://www.tiktok.com/", com.novatube.app.data.model.SearchKind.VIDEO, "TikTok"),
            SearchResult("yt-4", "Top 100 Music Hits", "Charts", 8000L, null, "https://www.youtube.com/results?search_query=top+100+music", com.novatube.app.data.model.SearchKind.VIDEO, "YouTube"),
            SearchResult("yt-5", "Live Concerts Now", "LiveX", 5500L, null, "https://www.twitch.tv/directory/category/music", com.novatube.app.data.model.SearchKind.VIDEO, "Twitch")
        )
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            Box(modifier = Modifier.width(220.dp)) {
                MediaCard(
                    title = item.title,
                    uploader = item.uploader,
                    duration = item.duration,
                    thumbnail = item.thumbnail,
                    onClick = { onItemClick(item.url) }
                )
            }
        }
    }
}

@Composable
private fun PlatformsGrid(onClick: (String) -> Unit) {
    val platforms = remember {
        listOf(
            "YouTube" to "https://www.youtube.com",
            "SoundCloud" to "https://soundcloud.com",
            "Vimeo" to "https://vimeo.com",
            "TikTok" to "https://www.tiktok.com",
            "Instagram" to "https://www.instagram.com",
            "Twitter" to "https://twitter.com",
            "Facebook" to "https://www.facebook.com",
            "Twitch" to "https://www.twitch.tv",
            "Reddit" to "https://www.reddit.com",
            "Dailymotion" to "https://www.dailymotion.com",
            "Pinterest" to "https://www.pinterest.com",
            "Vevo" to "https://www.vevo.com"
        )
    }
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(platforms) { (name, url) ->
            Card(
                onClick = { onClick(url) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            name.first().toString(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
