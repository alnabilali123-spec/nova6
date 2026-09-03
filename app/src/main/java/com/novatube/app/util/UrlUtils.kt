package com.novatube.app.util

import android.content.Context
import android.net.Uri
import java.net.URLDecoder
import java.util.regex.Pattern

object UrlUtils {

    private val URL_REGEX = Pattern.compile(
        "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
        Pattern.CASE_INSENSITIVE
    )

    fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val m = URL_REGEX.matcher(text)
        return if (m.find()) m.group(1) else null
    }

    fun allUrls(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        val m = URL_REGEX.matcher(text)
        val out = mutableListOf<String>()
        while (m.find()) out += m.group(1) ?: continue
        return out.distinct()
    }

    fun decode(url: String): String = runCatching { URLDecoder.decode(url, "UTF-8") }.getOrDefault(url)

    fun looksLikeMediaUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase()
        val hosts = listOf(
            "youtube.com", "youtu.be", "music.youtube.com",
            "soundcloud.com", "vimeo.com", "twitch.tv",
            "twitter.com", "x.com", "t.co",
            "tiktok.com", "instagram.com", "facebook.com", "fb.watch",
            "dailymotion.com", "dai.ly",
            "reddit.com", "redgifs.com",
            "pinterest.com", "pin.it"
        )
        return hosts.any { lower.contains(it) }
    }

    fun platformName(url: String?): String {
        if (url.isNullOrBlank()) return "Web"
        val lower = url.lowercase()
        return when {
            "youtube.com" in lower || "youtu.be" in lower -> "YouTube"
            "soundcloud.com" in lower -> "SoundCloud"
            "vimeo.com" in lower -> "Vimeo"
            "twitch.tv" in lower -> "Twitch"
            "twitter.com" in lower || "x.com" in lower -> "Twitter"
            "tiktok.com" in lower -> "TikTok"
            "instagram.com" in lower -> "Instagram"
            "facebook.com" in lower || "fb.watch" in lower -> "Facebook"
            "dailymotion.com" in lower || "dai.ly" in lower -> "Dailymotion"
            "reddit.com" in lower -> "Reddit"
            "redgifs.com" in lower -> "RedGifs"
            "pinterest.com" in lower || "pin.it" in lower -> "Pinterest"
            else -> "Web"
        }
    }

    fun host(url: String?): String? = runCatching { Uri.parse(url).host }.getOrNull()

    /** Read text from the system clipboard. */
    fun readClipboardText(context: Context): String? {
        return runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            cm?.primaryClip?.getItemAt(0)?.text?.toString()
        }.getOrNull()
    }
}
