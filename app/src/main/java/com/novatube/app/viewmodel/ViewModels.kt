package com.novatube.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novatube.app.NovaTubeApp
import com.novatube.app.data.entity.DownloadEntity
import com.novatube.app.data.entity.DownloadStatus
import com.novatube.app.data.model.MediaFormat
import com.novatube.app.data.model.MediaInfo
import com.novatube.app.data.model.RequestedDownload
import com.novatube.app.data.model.SearchKind
import com.novatube.app.data.model.SearchResult
import com.novatube.app.download.DownloadScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val app: NovaTubeApp) : AndroidViewModel(app) {
    val activeCount = app.downloadRepository.observeActiveCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val recent = app.downloadRepository.observeCompleted()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val totalSize = app.downloadRepository.observeTotalSize()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val engineState = app.ytDlpEngine.state
}

class DownloadsViewModel(private val app: NovaTubeApp) : AndroidViewModel(app) {
    val active = app.downloadRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val completed = app.downloadRepository.observeCompleted()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val failed = app.downloadRepository.observeFailed()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _batchUrls = MutableStateFlow<List<String>>(emptyList())
    val batchUrls: StateFlow<List<String>> = _batchUrls.asStateFlow()

    fun setBatch(urls: List<String>) { _batchUrls.value = urls.filter { it.isNotBlank() }.distinct() }
    fun clearBatch() { _batchUrls.value = emptyList() }

    fun retry(entity: DownloadEntity) {
        viewModelScope.launch {
            app.downloadRepository.updateStatus(entity.id, DownloadStatus.QUEUED)
            DownloadScheduler.enqueue(app, entity.id)
        }
    }
    fun cancel(entity: DownloadEntity) {
        viewModelScope.launch {
            DownloadScheduler.cancel(app, entity.id)
            app.downloadRepository.updateStatus(entity.id, DownloadStatus.CANCELLED)
        }
    }
    fun delete(entity: DownloadEntity) {
        viewModelScope.launch { app.downloadRepository.delete(entity.id) }
    }
    fun clearCompleted() {
        viewModelScope.launch { app.downloadRepository.clearCompleted() }
    }
    fun rename(entity: DownloadEntity, newName: String) {
        viewModelScope.launch { app.downloadRepository.rename(entity.id, newName) }
    }
    fun enqueueBatch(urls: List<String>) {
        viewModelScope.launch {
            for (url in urls) {
                // First do a quick info extract to learn metadata, then enqueue.
                val info = app.ytDlpEngine.extract(url)
                val request = RequestedDownload(
                    url = url,
                    formatId = "bestvideo*+bestaudio/best",
                    fileName = info?.title ?: "media",
                    isAudioOnly = false,
                    title = info?.title,
                    uploader = info?.uploader,
                    thumbnail = info?.thumbnail,
                    duration = info?.duration,
                    webpageUrl = info?.webpageUrl ?: url
                )
                val id = app.downloadRepository.enqueue(request)
                DownloadScheduler.enqueue(app, id)
            }
        }
    }
}

data class SearchUiState(
    val query: String = "",
    val source: String = "yt",
    val isLoading: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val error: String? = null,
    val filter: SearchKind? = null
)

class SearchViewModel(private val app: NovaTubeApp) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    val recent = app.searchHistoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setQuery(q: String) { _state.value = _state.value.copy(query = q) }
    fun setSource(s: String) { _state.value = _state.value.copy(source = s, results = emptyList()) }
    fun setFilter(f: SearchKind?) { _state.value = _state.value.copy(filter = f) }

    fun search(query: String = _state.value.query, force: Boolean = false) {
        val q = query.trim()
        if (q.isBlank()) {
            _state.value = _state.value.copy(results = emptyList(), suggestions = emptyList(), isLoading = false)
            return
        }
        _state.value = _state.value.copy(query = q, isLoading = true, error = null)
        viewModelScope.launch {
            app.searchHistoryRepository.add(q)
            val results = app.ytDlpEngine.search(q, source = _state.value.source, max = 30)
            val suggestions = if (results.isEmpty()) app.ytDlpEngine.titleSuggestions(q, 8) else emptyList()
            _state.value = _state.value.copy(
                isLoading = false,
                results = results,
                suggestions = suggestions
            )
        }
    }

    fun suggest(q: String) {
        if (q.length < 2) {
            _state.value = _state.value.copy(suggestions = emptyList())
            return
        }
        viewModelScope.launch {
            val s = app.ytDlpEngine.titleSuggestions(q, 6)
            _state.value = _state.value.copy(suggestions = s)
        }
    }
}

class LibraryViewModel(private val app: NovaTubeApp) : AndroidViewModel(app) {
    val audio = app.downloadRepository.observeAudio()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val video = app.downloadRepository.observeVideo()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun delete(entity: DownloadEntity) { viewModelScope.launch { app.downloadRepository.delete(entity.id) } }
    fun rename(entity: DownloadEntity, n: String) { viewModelScope.launch { app.downloadRepository.rename(entity.id, n) } }
}

class BrowserViewModel(private val app: NovaTubeApp) : AndroidViewModel(app) {
    val history = app.historyRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val bookmarks = app.bookmarkRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun addBookmark(title: String, url: String, favicon: String? = null) {
        viewModelScope.launch { app.bookmarkRepository.add(title, url, favicon) }
    }
    fun removeBookmark(id: Long) { viewModelScope.launch { app.bookmarkRepository.remove(id) } }
    fun addHistory(title: String, url: String, thumbnail: String? = null, uploader: String? = null, duration: Long? = null, platform: String? = null) {
        viewModelScope.launch { app.historyRepository.add(title, url, thumbnail, uploader, duration, platform) }
    }
    fun clearHistory() { viewModelScope.launch { app.historyRepository.clear() } }
}

data class FormatUiState(
    val url: String = "",
    val source: String = "direct",
    val isLoading: Boolean = true,
    val info: MediaInfo? = null,
    val videoFormats: List<MediaFormat> = emptyList(),
    val audioFormats: List<MediaFormat> = emptyList(),
    val error: String? = null,
    val selectedAudio: String = "mp3"
)

class FormatSelectionViewModel(private val app: NovaTubeApp) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(FormatUiState())
    val state: StateFlow<FormatUiState> = _state.asStateFlow()

    fun load(url: String, source: String = "direct") {
        _state.value = FormatUiState(url = url, source = source, isLoading = true)
        viewModelScope.launch {
            val info = app.ytDlpEngine.extract(url)
            val videos = app.ytDlpEngine.videoFormats(url)
            val audios = app.ytDlpEngine.audioFormats(url)
            _state.value = _state.value.copy(
                isLoading = false,
                info = info,
                videoFormats = videos,
                audioFormats = audios
            )
        }
    }

    fun setAudioFormat(fmt: String) { _state.value = _state.value.copy(selectedAudio = fmt) }

    fun enqueue(format: MediaFormat, isAudio: Boolean, onCreated: (Long) -> Unit = {}) {
        val s = _state.value
        val info = s.info ?: return
        val request = RequestedDownload(
            url = s.url,
            formatId = format.formatId ?: "best",
            fileName = (info.title ?: "media").take(80),
            isAudioOnly = isAudio,
            audioFormat = s.selectedAudio,
            title = info.title,
            uploader = info.uploader,
            thumbnail = info.thumbnail,
            duration = info.duration,
            webpageUrl = info.webpageUrl ?: s.url
        )
        viewModelScope.launch {
            val id = app.downloadRepository.enqueue(request)
            DownloadScheduler.enqueue(app, id)
            onCreated(id)
        }
    }

    fun quickBestVideo(onCreated: (Long) -> Unit = {}) {
        val s = _state.value
        val info = s.info ?: return
        val request = RequestedDownload(
            url = s.url,
            formatId = "bestvideo*+bestaudio/best",
            fileName = (info.title ?: "video").take(80),
            isAudioOnly = false,
            audioFormat = s.selectedAudio,
            title = info.title, uploader = info.uploader, thumbnail = info.thumbnail,
            duration = info.duration, webpageUrl = info.webpageUrl ?: s.url
        )
        viewModelScope.launch {
            val id = app.downloadRepository.enqueue(request)
            DownloadScheduler.enqueue(app, id); onCreated(id)
        }
    }
    fun quickBestAudio(onCreated: (Long) -> Unit = {}) {
        val s = _state.value
        val info = s.info ?: return
        val request = RequestedDownload(
            url = s.url,
            formatId = "bestaudio/best",
            fileName = (info.title ?: "audio").take(80),
            isAudioOnly = true,
            audioFormat = s.selectedAudio,
            title = info.title, uploader = info.uploader, thumbnail = info.thumbnail,
            duration = info.duration, webpageUrl = info.webpageUrl ?: s.url
        )
        viewModelScope.launch {
            val id = app.downloadRepository.enqueue(request)
            DownloadScheduler.enqueue(app, id); onCreated(id)
        }
    }
}

class SettingsViewModel(private val app: NovaTubeApp) : AndroidViewModel(app) {
    val prefs = app.preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.novatube.app.data.prefs.AppPreferences())
    val engineState = app.ytDlpEngine.state

    fun setTheme(mode: com.novatube.app.data.prefs.ThemeMode) = viewModelScope.launch { app.preferencesRepository.setThemeMode(mode) }
    fun setVideoQuality(q: com.novatube.app.data.prefs.VideoQuality) = viewModelScope.launch { app.preferencesRepository.setVideoQuality(q) }
    fun setAudioFormat(a: com.novatube.app.data.prefs.AudioFormat) = viewModelScope.launch { app.preferencesRepository.setAudioFormat(a) }
    fun setWifiOnly(v: Boolean) = viewModelScope.launch { app.preferencesRepository.setWifiOnly(v) }
    fun setClipboard(v: Boolean) = viewModelScope.launch { app.preferencesRepository.setClipboardDetection(v) }
    fun setNotifications(v: Boolean) = viewModelScope.launch { app.preferencesRepository.setNotifications(v) }
    fun setMaxParallel(n: Int) = viewModelScope.launch { app.preferencesRepository.setMaxParallel(n) }
    fun setLanguage(lang: String) = viewModelScope.launch { app.preferencesRepository.setLanguage(lang) }
    fun setEmbedMetadata(v: Boolean) = viewModelScope.launch { app.preferencesRepository.setEmbedMetadata(v) }
    fun setEmbedSubtitles(v: Boolean) = viewModelScope.launch { app.preferencesRepository.setEmbedSubtitles(v) }
    fun setSubtitleLang(lang: String) = viewModelScope.launch { app.preferencesRepository.setSubtitleLang(lang) }
    fun setPlaybackSpeed(s: Float) = viewModelScope.launch { app.preferencesRepository.setPlaybackSpeed(s) }

    fun clearSearchHistory() = viewModelScope.launch { app.searchHistoryRepository.clear() }
    fun clearBrowserHistory() = viewModelScope.launch { app.historyRepository.clear() }
    fun clearCompleted() = viewModelScope.launch { app.downloadRepository.clearCompleted() }
    fun clearAllDownloads() = viewModelScope.launch { app.downloadRepository.clearAll() }
    fun reinitEngine() = viewModelScope.launch { app.ytDlpEngine.init() }
}

class PlaylistsViewModel(private val app: NovaTubeApp) : AndroidViewModel(app) {
    val playlists = app.playlistRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun create(name: String, description: String? = null) = viewModelScope.launch { app.playlistRepository.create(name, description) }
    fun delete(id: Long) = viewModelScope.launch { app.playlistRepository.delete(id) }
    fun rename(id: Long, name: String) = viewModelScope.launch {
        val current = app.playlistRepository.get(id) ?: return@launch
        app.playlistRepository.update(id, name, current.description)
    }
}
