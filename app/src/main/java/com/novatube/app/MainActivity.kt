package com.novatube.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.novatube.app.nav.Route
import com.novatube.app.ui.components.AppDrawer
import com.novatube.app.ui.screens.browser.BrowserScreen
import com.novatube.app.ui.screens.downloads.DownloadsScreen
import com.novatube.app.ui.screens.format.FormatSelectionScreen
import com.novatube.app.ui.screens.history.HistoryScreen
import com.novatube.app.ui.screens.home.HomeScreen
import com.novatube.app.ui.screens.library.LibraryScreen
import com.novatube.app.ui.screens.music.MusicScreen
import com.novatube.app.ui.screens.onboarding.OnboardingScreen
import com.novatube.app.ui.screens.player.PlayerScreen
import com.novatube.app.ui.screens.playlists.PlaylistsScreen
import com.novatube.app.ui.screens.search.SearchScreen
import com.novatube.app.ui.screens.settings.SettingsScreen
import com.novatube.app.ui.theme.NovaTubeTheme
import com.novatube.app.util.UrlUtils
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> Log.i(TAG, "Permission result: $results") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        requestPermissionsIfNeeded()
        setContent {
            val app = applicationContext as NovaTubeApp
            val prefs by app.preferencesRepository.preferences.collectAsState(initial = com.novatube.app.data.prefs.AppPreferences())
            NovaTubeTheme(themeMode = prefs.themeMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot()
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.READ_MEDIA_AUDIO
                )
            )
        }
    }

    @Composable
    private fun AppRoot() {
        val app = applicationContext as NovaTubeApp
        var onboardingDone by remember { mutableStateOf<Boolean?>(null) }

        LaunchedEffect(Unit) {
            val current = app.preferencesRepository.preferences.first()
            // We use the existence of a non-default language to mark "onboarded".
            onboardingDone = current.language != "system"
        }

        val state = onboardingDone
        when (state) {
            null -> { /* wait */ }
            false -> OnboardingScreen(onDone = { onboardingDone = true })
            true -> MainScaffold()
        }
    }

    @Composable
    private fun MainScaffold() {
        val nav = rememberNavController()
        val backStack by nav.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route
        val initialUrl = remember {
            intent?.let { extractUrlFromIntent(it) }
        }
        LaunchedEffect(initialUrl) {
            if (!initialUrl.isNullOrBlank()) {
                nav.navigate(Route.Format.build(initialUrl, "share"))
            }
        }

        AppDrawer(
            activeRoute = currentRoute,
            onNavigate = { route ->
                nav.navigate(route) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        ) {
            NavHost(
                navController = nav,
                startDestination = "home",
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                composable("home") {
                    HomeScreen(
                        paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        onNavigateToSearch = { q -> nav.navigate(Route.Search.withQuery(q)) },
                        onNavigateToFormat = { url -> nav.navigate(Route.Format.build(url, "direct")) },
                        onNavigateToDownloads = { nav.navigate("downloads") },
                        onNavigateToLibrary = { nav.navigate("library") },
                        onNavigateToBrowser = { nav.navigate(Route.Browser.with()) },
                        onNavigateToHistory = { nav.navigate("history") },
                        onNavigateToMusic = { nav.navigate("music") },
                        onNavigateToSettings = { nav.navigate("settings") },
                        onPlay = { path, title -> nav.navigate(Route.Player.build(path, title, "local")) }
                    )
                }
                composable(
                    route = "search?query={query}",
                    arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })
                ) { entry ->
                    val q = entry.arguments?.getString("query")?.takeIf { it.isNotBlank() }
                    SearchScreen(
                        paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        initialQuery = q,
                        onBack = { nav.popBackStack() },
                        onResultClick = { res -> nav.navigate(Route.Format.build(res.url, "search")) }
                    )
                }
                composable("downloads") {
                    DownloadsScreen(
                        paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        onOpenLocal = { p, t -> nav.navigate(Route.Player.build(p, t, "local")) }
                    )
                }
                composable("library") {
                    LibraryScreen(
                        paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        onPlayAudio = { p, t -> nav.navigate(Route.Player.build(p, t, "local")) },
                        onPlayVideo = { p, t -> nav.navigate(Route.Player.build(p, t, "local")) }
                    )
                }
                composable("music") {
                    MusicScreen(paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp))
                }
                composable(
                    route = Route.Player.path,
                    arguments = listOf(
                        navArgument("url") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType; defaultValue = "" },
                        navArgument("type") { type = NavType.StringType; defaultValue = "local" }
                    )
                ) { entry ->
                    val url = java.net.URLDecoder.decode(entry.arguments?.getString("url") ?: "", "UTF-8")
                    val title = java.net.URLDecoder.decode(entry.arguments?.getString("title") ?: "", "UTF-8")
                    val type = entry.arguments?.getString("type") ?: "local"
                    PlayerScreen(
                        paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        url = url, title = title, type = type,
                        onBack = { nav.popBackStack() }
                    )
                }
                composable(
                    route = Route.Format.path,
                    arguments = listOf(
                        navArgument("url") { type = NavType.StringType },
                        navArgument("source") { type = NavType.StringType; defaultValue = "direct" }
                    )
                ) { entry ->
                    val url = java.net.URLDecoder.decode(entry.arguments?.getString("url") ?: "", "UTF-8")
                    FormatSelectionScreen(
                        paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        url = url,
                        onBack = { nav.popBackStack() },
                        onEnqueued = { nav.popBackStack(); nav.navigate("downloads") }
                    )
                }
                composable(
                    route = Route.Browser.path,
                    arguments = listOf(navArgument("url") { type = NavType.StringType; defaultValue = "" })
                ) { entry ->
                    val url = entry.arguments?.getString("url")?.takeIf { it.isNotBlank() }
                    BrowserScreen(
                        paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        initialUrl = url,
                        onExtractLink = { u -> nav.navigate(Route.Format.build(u, "browser")) },
                        onBack = { nav.popBackStack() }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        onBack = { nav.popBackStack() }
                    )
                }
                composable("playlists") {
                    PlaylistsScreen(
                        paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        onBack = { nav.popBackStack() }
                    )
                }
                composable("history") {
                    HistoryScreen(
                        paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        onBack = { nav.popBackStack() }
                    ) { url -> nav.navigate(Route.Format.build(url, "history")) }
                }
            }
        }
    }

    private fun extractUrlFromIntent(intent: Intent): String? {
        if (intent.action == Intent.ACTION_VIEW) intent.data?.toString()?.let { return it }
        if (intent.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                UrlUtils.extractFirstUrl(text)?.let { return it }
            }
        }
        return null
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
