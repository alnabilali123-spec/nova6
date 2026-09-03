package com.novatube.app.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.novatube.app.util.OkHttpProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Pure-Kotlin / OkHttp / Android-Bitmap image loader — no third-party deps. */
@Composable
fun AsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var bitmap: ImageBitmap? by remember(url) { mutableStateOf(null) }
    var failed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val req = okhttp3.Request.Builder().url(url).build()
                OkHttpProvider.client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching null
                    val bytes = resp.body?.bytes() ?: return@runCatching null
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }.getOrNull()
        }
        if (result != null) bitmap = result.asImageBitmap() else failed = true
    }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
        if (bitmap == null && failed) {
            Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray))
        }
    }
}

fun formatDuration(seconds: Long?): String {
    if (seconds == null || seconds <= 0) return "—"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}
