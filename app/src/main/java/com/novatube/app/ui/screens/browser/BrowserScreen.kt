@file:OptIn(ExperimentalMaterial3Api::class)

package com.novatube.app.ui.screens.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.JavaScript
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novatube.app.util.UrlUtils
import com.novatube.app.viewmodel.BrowserViewModel
import com.novatube.app.viewmodel.ViewModelFactory

private data class BrowserTab(
    val id: Int,
    var url: String,
    var title: String,
    var webView: WebView? = null
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    paddingValues: PaddingValues,
    initialUrl: String? = null,
    onExtractLink: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: BrowserViewModel = viewModel(factory = ViewModelFactory.from(context))
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    val tabs: SnapshotStateList<BrowserTab> = remember { mutableStateListOf() }
    var currentIndex by remember { mutableStateOf(0) }
    var urlInput by remember { mutableStateOf("https://www.google.com") }
    var showBookmarks by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var desktopMode by remember { mutableStateOf(false) }
    var jsEnabled by remember { mutableStateOf(true) }
    var currentTitle by remember { mutableStateOf("Browser") }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            val normalized = if (initialUrl.startsWith("http", ignoreCase = true)) initialUrl
            else "https://${initialUrl}"
            val w = createWebView(
                context = context,
                url = normalized,
                titleUpdate = { t -> currentTitle = t },
                progressUpdate = {},
                urlUpdate = { u -> urlInput = u; vm.addHistory(currentTitle.ifBlank { u }, u) },
                downloadIntercept = onExtractLink,
                javascript = jsEnabled,
                desktop = desktopMode
            )
            tabs.add(BrowserTab(id = tabs.size, url = normalized, title = normalized, webView = w))
            currentIndex = tabs.lastIndex
            urlInput = normalized
        } else if (tabs.isEmpty()) {
            val w = createWebView(
                context = context,
                url = "https://www.google.com",
                titleUpdate = { t -> currentTitle = t },
                progressUpdate = {},
                urlUpdate = { u -> urlInput = u },
                downloadIntercept = onExtractLink,
                javascript = jsEnabled,
                desktop = desktopMode
            )
            tabs.add(BrowserTab(id = 0, url = "https://www.google.com", title = "Google", webView = w))
            currentIndex = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    val target = if (urlInput.startsWith("http", ignoreCase = true)) urlInput else "https://${urlInput}"
                    tabs.getOrNull(currentIndex)?.webView?.loadUrl(target)
                }),
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (urlInput.isNotBlank()) {
                        IconButton(onClick = { urlInput = "" }) { Icon(Icons.Outlined.Close, contentDescription = null) }
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
            IconButton(onClick = { showBookmarks = !showBookmarks }) { Icon(Icons.Outlined.Bookmarks, contentDescription = "Bookmarks") }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "Menu") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (desktopMode) "Mobile mode" else "Desktop mode") },
                        onClick = { desktopMode = !desktopMode; showMenu = false; reloadActive(tabs, currentIndex, desktopMode, jsEnabled) },
                        leadingIcon = { Icon(Icons.Outlined.DesktopWindows, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (jsEnabled) "Disable JavaScript" else "Enable JavaScript") },
                        onClick = { jsEnabled = !jsEnabled; showMenu = false; reloadActive(tabs, currentIndex, desktopMode, jsEnabled) },
                        leadingIcon = { Icon(Icons.Outlined.JavaScript, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Open externally") },
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlInput))
                            runCatching { context.startActivity(intent) }
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Bookmark page") },
                        onClick = {
                            vm.addBookmark(currentTitle.ifBlank { urlInput }, urlInput)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.Star, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Download this page") },
                        onClick = { onExtractLink(urlInput); showMenu = false },
                        leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) }
                    )
                }
            }
        }
        if (showBookmarks) {
            BookmarksPanel(
                bookmarks = bookmarks.map { it.title to it.url },
                history = history.map { it.title to it.url }.take(15),
                onPick = { u ->
                    urlInput = u
                    tabs.getOrNull(currentIndex)?.webView?.loadUrl(u)
                    showBookmarks = false
                },
                onClose = { showBookmarks = false },
                onClearHistory = { vm.clearHistory() }
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            // Render the active tab's WebView; keep inactive tabs in the list so they keep state.
            tabs.getOrNull(currentIndex)?.webView?.let { w ->
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { w },
                    update = { view -> view.visibility = android.view.View.VISIBLE }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { tabs.getOrNull(currentIndex)?.webView?.goBack() }) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            IconButton(onClick = { tabs.getOrNull(currentIndex)?.webView?.goForward() }) {
                Icon(Icons.Outlined.ArrowForward, contentDescription = "Forward")
            }
            IconButton(onClick = { tabs.getOrNull(currentIndex)?.webView?.reload() }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Reload")
            }
            Spacer(modifier = Modifier.weight(1f))
            AssistChip(
                onClick = { /* noop */ },
                label = { Text("Tab ${currentIndex + 1}/${tabs.size}") },
                leadingIcon = { Icon(Icons.Outlined.JavaScript, contentDescription = null) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = {
                val w = createWebView(
                    context = context,
                    url = "https://www.google.com",
                    titleUpdate = { currentTitle = it },
                    progressUpdate = {},
                    urlUpdate = { urlInput = it },
                    downloadIntercept = onExtractLink,
                    javascript = jsEnabled,
                    desktop = desktopMode
                )
                tabs.add(BrowserTab(id = tabs.size, url = "https://www.google.com", title = "New Tab", webView = w))
                currentIndex = tabs.lastIndex
            }) { Icon(Icons.Outlined.Add, contentDescription = "New tab") }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: android.content.Context,
    url: String,
    titleUpdate: (String) -> Unit,
    progressUpdate: (Int) -> Unit,
    urlUpdate: (String) -> Unit,
    downloadIntercept: (String) -> Unit,
    javascript: Boolean,
    desktop: Boolean
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        settings.javaScriptEnabled = javascript
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.userAgentString = if (desktop) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        } else settings.userAgentString
        settings.mediaPlaybackRequiresUserGesture = false
        CookieManager.getInstance().setAcceptCookie(true)
        webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) { titleUpdate(title ?: "") }
            override fun onProgressChanged(view: WebView?, newProgress: Int) { progressUpdate(newProgress) }
        }
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val target = uri.toString()
                urlUpdate(target)
                return false
            }
        }
        setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            // Try to send media URL through our pipeline so it ends up in the
            // format selection screen.
            if (URLUtil.isValidUrl(url)) downloadIntercept(url)
        })
        loadUrl(url)
    }
}

private fun reloadActive(
    tabs: SnapshotStateList<BrowserTab>,
    currentIndex: Int,
    desktop: Boolean,
    js: Boolean
) {
    tabs.getOrNull(currentIndex)?.let { tab ->
        tab.webView?.settings?.userAgentString = if (desktop) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        } else tab.webView?.settings?.userAgentString
        tab.webView?.settings?.javaScriptEnabled = js
        tab.webView?.reload()
    }
}

@Composable
private fun BookmarksPanel(
    bookmarks: List<Pair<String, String>>,
    history: List<Pair<String, String>>,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
    onClearHistory: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bookmarks", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Close") }
            }
            if (bookmarks.isEmpty()) Text("No bookmarks yet")
            bookmarks.forEach { (title, url) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    IconButton(onClick = { onPick(url) }) { Icon(Icons.Outlined.OpenInBrowser, contentDescription = null) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("History", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onClearHistory) { Icon(Icons.Outlined.History, contentDescription = "Clear") }
            }
            history.take(8).forEach { (title, url) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.History, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title.ifBlank { url }, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
    }
}
