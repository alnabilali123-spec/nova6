package com.novatube.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "novatube_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class VideoQuality { BEST, HIGH_1080, MEDIUM_720, LOW_480, AUDIO_ONLY }
enum class AudioFormat { MP3, M4A, OPUS, WAV, FLAC }

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val videoQuality: VideoQuality = VideoQuality.BEST,
    val audioFormat: AudioFormat = AudioFormat.MP3,
    val wifiOnly: Boolean = false,
    val clipboardDetection: Boolean = true,
    val desktopMode: Boolean = false,
    val javascriptEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val maxParallelDownloads: Int = 3,
    val language: String = "system", // "system" | "en" | "ar"
    val embedMetadata: Boolean = true,
    val embedSubtitles: Boolean = false,
    val subtitleLang: String = "en,ar",
    val darkModeOverride: Boolean? = null, // null = follow system
    val playbackSpeed: Float = 1.0f,
    val lastSharedUrl: String? = null
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val AUDIO_FORMAT = stringPreferencesKey("audio_format")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val CLIPBOARD = booleanPreferencesKey("clipboard_detection")
        val DESKTOP_MODE = booleanPreferencesKey("desktop_mode")
        val JAVASCRIPT = booleanPreferencesKey("javascript_enabled")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val MAX_PARALLEL = intPreferencesKey("max_parallel_downloads")
        val LANGUAGE = stringPreferencesKey("language")
        val EMBED_METADATA = booleanPreferencesKey("embed_metadata")
        val EMBED_SUBTITLES = booleanPreferencesKey("embed_subtitles")
        val SUBTITLE_LANG = stringPreferencesKey("subtitle_lang")
        val DARK_OVERRIDE = booleanPreferencesKey("dark_mode_override")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val LAST_SHARED = stringPreferencesKey("last_shared_url")
    }

    val preferences: Flow<AppPreferences> = context.appDataStore.data.map { p ->
        AppPreferences(
            themeMode = parseEnum(p[Keys.THEME], ThemeMode.SYSTEM) { ThemeMode.valueOf(it) },
            videoQuality = parseEnum(p[Keys.VIDEO_QUALITY], VideoQuality.BEST) { VideoQuality.valueOf(it) },
            audioFormat = parseEnum(p[Keys.AUDIO_FORMAT], AudioFormat.MP3) { AudioFormat.valueOf(it) },
            wifiOnly = p[Keys.WIFI_ONLY] ?: false,
            clipboardDetection = p[Keys.CLIPBOARD] ?: true,
            desktopMode = p[Keys.DESKTOP_MODE] ?: false,
            javascriptEnabled = p[Keys.JAVASCRIPT] ?: true,
            notificationsEnabled = p[Keys.NOTIFICATIONS] ?: true,
            maxParallelDownloads = p[Keys.MAX_PARALLEL] ?: 3,
            language = p[Keys.LANGUAGE] ?: "system",
            embedMetadata = p[Keys.EMBED_METADATA] ?: true,
            embedSubtitles = p[Keys.EMBED_SUBTITLES] ?: false,
            subtitleLang = p[Keys.SUBTITLE_LANG] ?: "en,ar",
            darkModeOverride = p[Keys.DARK_OVERRIDE],
            playbackSpeed = p[Keys.PLAYBACK_SPEED] ?: 1.0f,
            lastSharedUrl = p[Keys.LAST_SHARED]
        )
    }

    private inline fun <T> parseEnum(value: String?, default: T, parser: (String) -> T): T =
        if (value == null) default else runCatching { parser(value) }.getOrDefault(default)

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }
    suspend fun setVideoQuality(q: VideoQuality) = edit { it[Keys.VIDEO_QUALITY] = q.name }
    suspend fun setAudioFormat(a: AudioFormat) = edit { it[Keys.AUDIO_FORMAT] = a.name }
    suspend fun setWifiOnly(v: Boolean) = edit { it[Keys.WIFI_ONLY] = v }
    suspend fun setClipboardDetection(v: Boolean) = edit { it[Keys.CLIPBOARD] = v }
    suspend fun setDesktopMode(v: Boolean) = edit { it[Keys.DESKTOP_MODE] = v }
    suspend fun setJavascript(v: Boolean) = edit { it[Keys.JAVASCRIPT] = v }
    suspend fun setNotifications(v: Boolean) = edit { it[Keys.NOTIFICATIONS] = v }
    suspend fun setMaxParallel(n: Int) = edit { it[Keys.MAX_PARALLEL] = n }
    suspend fun setLanguage(lang: String) = edit { it[Keys.LANGUAGE] = lang }
    suspend fun setEmbedMetadata(v: Boolean) = edit { it[Keys.EMBED_METADATA] = v }
    suspend fun setEmbedSubtitles(v: Boolean) = edit { it[Keys.EMBED_SUBTITLES] = v }
    suspend fun setSubtitleLang(lang: String) = edit { it[Keys.SUBTITLE_LANG] = lang }
    suspend fun setDarkOverride(v: Boolean?) = edit { if (v == null) it.remove(Keys.DARK_OVERRIDE) else it[Keys.DARK_OVERRIDE] = v }
    suspend fun setPlaybackSpeed(v: Float) = edit { it[Keys.PLAYBACK_SPEED] = v }
    suspend fun setLastSharedUrl(v: String?) = edit { if (v == null) it.remove(Keys.LAST_SHARED) else it[Keys.LAST_SHARED] = v }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.appDataStore.edit { block(it) }
    }
}
