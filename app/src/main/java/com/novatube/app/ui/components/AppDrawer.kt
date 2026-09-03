@file:OptIn(ExperimentalMaterial3Api::class)

package com.novatube.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novatube.app.R

data class DrawerItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val badge: String? = null
)

@Composable
fun AppDrawer(
    activeRoute: String?,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val items = listOf(
        DrawerItem("home", R.string.nav_home, Icons.Outlined.Home),
        DrawerItem("search", R.string.nav_search, Icons.Outlined.Search),
        DrawerItem("browser", R.string.nav_browser, Icons.Outlined.Public),
        DrawerItem("downloads", R.string.nav_downloads, Icons.Outlined.SlowMotionVideo),
        DrawerItem("library", R.string.nav_library, Icons.Outlined.LibraryBooks),
        DrawerItem("music", R.string.nav_music, Icons.Outlined.MusicNote),
        DrawerItem("playlists", R.string.nav_playlists, Icons.Outlined.PlaylistPlay),
        DrawerItem("history", R.string.nav_history, Icons.Outlined.History)
    )
    val bottomItems = listOf(
        DrawerItem("settings", R.string.nav_settings, Icons.Outlined.Settings)
    )

    androidx.compose.material3.ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    DrawerHeader()
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    items.forEach { item ->
                        val active = activeRoute?.startsWith(item.route) == true
                        NavigationDrawerItem(
                            label = { Text(stringResource(item.labelRes), fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal) },
                            selected = active,
                            onClick = { onNavigate(item.route) },
                            icon = { Icon(item.icon, contentDescription = null, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                            badge = item.badge?.let { { Text(it, color = MaterialTheme.colorScheme.primary) } },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    bottomItems.forEach { item ->
                        val active = activeRoute?.startsWith(item.route) == true
                        NavigationDrawerItem(
                            label = { Text(stringResource(item.labelRes)) },
                            selected = active,
                            onClick = { onNavigate(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    DrawerFooter()
                }
            }
        }
    ) {
        content()
    }
}

@Composable
private fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
    ) {
        Column(modifier = Modifier.padding(20.dp).align(Alignment.BottomStart)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Whatshot,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.app_name),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.app_tagline),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DrawerFooter() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Powered by yt-dlp · Media3 · Compose",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
