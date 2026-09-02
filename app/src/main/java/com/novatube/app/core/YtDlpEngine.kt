package com.novatube.app.core

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.novatube.app.data.model.MediaFormat
import com.novatube.app.data.model.MediaInfo
import com.novatube.app.data.model.SearchKind
import com.novatube.app.data.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Progress event emitted by the yt-dlp downloader.
 *
 * @param percent     0..100
 * @param totalBytes  total expected size when known
 * @param downloadedBytes bytes downloaded so far
 * @param speed       bytes/sec when reported by yt-dlp
 * @param etaSeconds  ETA in seconds when reported
 */
data class ProgressEvent(
    val percent: Float,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val speed: Long,
    val etaSeconds: Long,
    val phase: Phase
) {
    enum class { DOWNLOADING, POSTPROCESSING, FINISHED, ERROR }
}

/**
 * Wraps the yt-dlp binary shipped inside `assets/yt-dlp`. We extract it to the app's
 * private files dir on first use, mark it executable, and shell out via [ProcessBuilder].
 *
 * No JitPack / Maven dependency on `com.yausername.youtubedl` — the binary is the
 * dependency.
 */
class YtDlpEngine(private val context: Context) {

    private val gson = Gson()

    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state.asStateFlow()

    data class EngineState(
        val ready: Boolean = false,
        val version: String? = null,
        val initializing: Boolean = false,
        val lastError: String? = null
    )

    private val binaryFile: File by lazy {
        val outFile = File(context.filesDir, "yt-dlp")
        if (!outFile.exists() || outFile.length() < 1_000_000) {
            extractFromAssets(outFile)
        }
        runCatching { outFile.setExecutable(true) }
        outFile
    }

    private fun extractFromAssets(target: File) {
        Log.i(TAG, "Extracting yt-dlp from assets to ${target.absolutePath}")
        target.parentFile?.mkdirs()
        context.assets.open("yt-dlp").use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
        target.setExecutable(true)
        Log.i(TAG, "yt-dlp extracted: ${target.length()} bytes")
    }

    /** Make sure the binary is on disk and that yt-dlp responds. */
    suspend fun init() = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(initializing = true, lastError = null)
        try {
            // Touch the lazy field to ensure extraction happens.
            val f = binaryFile
            if (!f.canExecute()) {
                f.setExecutable(true)
            }
            val v = runProcess(listOf("--version"), timeoutMs = 30_000).trim()
            _state.value = EngineState(ready = v.isNotEmpty(), version = v, initializing = false, lastError = null)
            Log.i(TAG, "yt-dlp ready: $v")
        } catch (t: Throwable) {
            _state.value = EngineState(ready = false, version = null, initializing = false, lastError = t.message)
            Log.e(TAG, "yt-dlp init failed", t)
        }
    }

    /** Run `yt-dlp -J <url>` and parse the resulting JSON into [MediaInfo]. */
    suspend fun extract(url: String): MediaInfo? = withContext(Dispatchers.IO) {
        try {
            val json = runProcess(
                listOf(url, "-J", "--no-warnings", "--no-playlist", "--no-color"),
                timeoutMs = 90_000
            )
            gson.fromJson(json, MediaInfo::class.java)
        } catch (t: Throwable) {
            Log.e(TAG, "extract failed for $url", t)
            null
        }
    }

    /** Returns a sorted list of video formats (best first). */
    suspend fun videoFormats(url: String): List<MediaFormat> = withContext(Dispatchers.IO) {
        extract(url)?.formats?.filter { it.isVideo }
            ?.sortedWith(compareByDescending<MediaFormat> { it.height ?: 0 }.thenByDescending { it.tbr ?: 0.0 })
            ?: emptyList()
    }

    suspend fun audioFormats(url: String): List<MediaFormat> = withContext(Dispatchers.IO) {
        extract(url)?.formats?.filter { it.isAudio }
            ?.sortedByDescending { it.abr ?: 0.0 }
            ?: emptyList()
    }

    /**
     * Search by platform prefix. `source = "yt"` → `ytsearchN:`, `source = "sc"` → `scsearchN:`.
     * The function runs `yt-dlp --flat-playlist -J` and decodes the entries.
     */
    suspend fun search(query: String, source: String = "yt", max: Int = 25): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val prefix = when (source) {
                "sc" -> "scsearch$max"
                else -> "ytsearch$max"
            }
            val platform = when (source) {
                "sc" -> "SoundCloud"
                else -> "YouTube"
            }
            val kind = when (source) {
                "sc" -> SearchKind.AUDIO
                else -> SearchKind.VIDEO
            }
            try {
                val json = runProcess(
                    listOf("$prefix:$query", "--flat-playlist", "--skip-download", "-J", "--no-warnings", "--no-playlist"),
                    timeoutMs = 60_000
                )
                val root = gson.fromJson(json, JsonObject::class.java) ?: return@withContext emptyList()
                val entries = root.getAsJsonArray("entries") ?: return@withContext emptyList()
                entries.mapNotNull { el ->
                    val obj = el.asJsonObject
                    val id = obj.get("id")?.asString ?: return@mapNotNull null
                    val title = obj.get("title")?.asString ?: return@mapNotNull null
                    val webpage = obj.get("url")?.asString
                        ?: obj.get("webpage_url")?.asString
                        ?: "https://www.youtube.com/watch?v=$id"
                    val uploader = obj.get("uploader")?.asString ?: obj.get("uploader_id")?.asString
                    val duration = obj.get("duration")?.asLong
                    val viewCount = obj.get("view_count")?.asLong
                    val thumb = obj.get("thumbnails")?.asJsonArray
                        ?.lastOrNull()?.asJsonObject?.get("url")?.asString
                    SearchResult(
                        id = id,
                        title = title,
                        uploader = uploader,
                        duration = duration,
                        thumbnail = thumb,
                        url = webpage,
                        kind = kind,
                        platform = platform,
                        viewCount = viewCount
                    )
                }
            } catch (t: Throwable) {
                Log.e(TAG, "search failed", t)
                emptyList()
            }
        }

    /**
     * Title suggestions via `--print %(title)s` with `--flat-playlist`.
     */
    suspend fun titleSuggestions(query: String, max: Int = 8): List<String> = withContext(Dispatchers.IO) {
        try {
            val json = runProcess(
                listOf("ytsearch$max:$query", "--skip-download", "--flat-playlist", "--print", "%(title)s", "--no-warnings"),
                timeoutMs = 30_000
            )
            json.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.take(max)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Download the given URL into [outputDir] using [filename] (extension is appended by yt-dlp
     * via `%(ext)s`). If [audioOnly] is true the file is extracted to [audioFormat] (default mp3).
     *
     * Returns the produced file or null on failure.  [onProgress] fires for every `[download]`
     * line yt-dlp prints and once on completion / error.
     */
    suspend fun download(
        url: String,
        outputDir: File,
        filename: String,
        formatId: String? = null,
        audioOnly: Boolean = false,
        audioFormat: String = "mp3",
        embedMetadata: Boolean = true,
        embedThumbnail: Boolean = false,
        writeSubtitles: Boolean = false,
        subtitleLang: String = "en",
        onProgress: (ProgressEvent) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        if (!_state.value.ready) init()
        if (!_state.value.ready) {
            onProgress(ProgressEvent(0f, 0, 0, 0, 0, ProgressEvent.Phase.ERROR))
            return@withContext null
        }
        outputDir.mkdirs()
        val safeName = filename.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "media" }
        val template = "${outputDir.absolutePath}/$safeName.%(ext)s"

        val args = buildList {
            add(url)
            add("-o"); add(template)
            add("--newline")
            add("--no-color")
            add("--no-warnings")
            add("--no-mtime")
            if (audioOnly) {
                add("-x")
                add("--audio-format"); add(audioFormat)
                add("--audio-quality"); add("0")
            } else if (!formatId.isNullOrBlank()) {
                add("-f"); add(formatId)
                add("--merge-output-format"); add("mp4")
            }
            if (embedMetadata) add("--add-metadata")
            if (embedThumbnail && audioOnly) add("--embed-thumbnail")
            if (writeSubtitles) {
                add("--write-subs")
                add("--write-auto-subs")
                add("--sub-langs"); add(subtitleLang)
                add("--convert-subs"); add("srt")
            }
        }

        val process = ProcessBuilder(binaryFile.absolutePath, *args.toTypedArray())
            .directory(context.filesDir)
            .redirectErrorStream(true)
            .start()

        // Read output in a daemon thread so we can observe progress live.
        val outputBuffer = StringBuilder()
        val readerThread = Thread {
            try {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        synchronized(outputBuffer) { outputBuffer.append(line).append("\n") }
                        parseProgressLine(line)?.let(onProgress)
                    }
                }
            } catch (_: Throwable) {}
        }
        readerThread.isDaemon = true
        readerThread.start()

        val exit = process.waitFor(6, TimeUnit.HOURS)
        if (!exit) {
            process.destroyForcibly()
            readerThread.join(5_000)
            onProgress(ProgressEvent(0f, 0, 0, 0, 0, ProgressEvent.Phase.ERROR))
            return@withContext null
        }
        readerThread.join(5_000)
        val output = synchronized(outputBuffer) { outputBuffer.toString() }
        if (process.exitValue() == 0) {
            // Find the produced file.
            val produced = outputDir.listFiles()
                ?.filter { it.name.startsWith(safeName) && it.length() > 0 }
                ?.maxByOrNull { it.lastModified() }
            if (produced != null) {
                onProgress(ProgressEvent(100f, produced.length(), produced.length(), 0, 0, ProgressEvent.Phase.FINISHED))
            }
            produced
        } else {
            Log.e(TAG, "yt-dlp exit ${process.exitValue()}: ${output.take(400)}")
            onProgress(ProgressEvent(0f, 0, 0, 0, 0, ProgressEvent.Phase.ERROR))
            null
        }
    }

    /**
     * Run an arbitrary yt-dlp invocation, capturing stdout+stderr. Used internally and by the
     * search/info APIs.
     */
    private fun runProcess(args: List<String>, timeoutMs: Long): String {
        val pb = ProcessBuilder(binaryFile.absolutePath, *args.toTypedArray())
            .directory(context.filesDir)
            .redirectErrorStream(true)
        val process = pb.start()
        val out = StringBuilder()
        val t = Thread {
            try {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { synchronized(out) { out.append(it).append("\n") } }
                }
            } catch (_: Throwable) {}
        }
        t.isDaemon = true
        t.start()
        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroyForcibly()
            t.join(2_000)
            throw RuntimeException("yt-dlp process timed out after ${timeoutMs}ms (args=$args)")
        }
        t.join(2_000)
        if (process.exitValue() != 0) {
            throw RuntimeException("yt-dlp exited ${process.exitValue()}: ${out.toString().take(400)}")
        }
        return out.toString()
    }

    // ---- progress parsing ----------------------------------------------------

    private val progressRegex: Pattern = Pattern.compile(
        "\\[download\\]\\s+(\\d+\\.?\\d*)%\\s+of\\s+(\\S+)" +
            "(?:\\s+at\\s+(\\S+))?" +
            "(?:\\s+ETA\\s+(\\S+))?"
    )

    private fun parseProgressLine(line: String): ProgressEvent? {
        val m = progressRegex.matcher(line)
        if (!m.find()) {
            // Post-processing / destination / error fall through to phase detection.
            return when {
                line.contains("[ExtractAudio]") || line.contains("[Merger]") || line.contains("[ffmpeg]") ->
                    ProgressEvent(99f, 0, 0, 0, 0, ProgressEvent.Phase.POSTPROCESSING)
                line.startsWith("ERROR:") || line.contains("Unable to extract") ->
                    ProgressEvent(0f, 0, 0, 0, 0, ProgressEvent.Phase.ERROR)
                else -> null
            }
        }
        val percent = m.group(1)?.toFloatOrNull() ?: return null
        val total = parseSize(m.group(2))
        val speed = parseSize(m.group(3))
        val eta = parseEta(m.group(4))
        return ProgressEvent(
            percent = percent,
            totalBytes = total,
            downloadedBytes = (percent / 100f * total).toLong(),
            speed = speed,
            etaSeconds = eta,
            phase = ProgressEvent.Phase.DOWNLOADING
        )
    }

    private fun parseSize(s: String?): Long {
        if (s.isNullOrBlank()) return 0L
        val regex = Regex("(?i)(\\d+\\.?\\d*)\\s*([KMGT]?I?B?)?")
        val match = regex.find(s.trim()) ?: return 0L
        val value = match.groupValues[1].toDoubleOrNull() ?: return 0L
        val unit = match.groupValues[2].uppercase().replace("IB", "B")
        val mult = when (unit.firstOrNull() ?: 'B') {
            'K' -> 1024L
            'M' -> 1024L * 1024
            'G' -> 1024L * 1024 * 1024
            'T' -> 1024L * 1024 * 1024 * 1024
            else -> 1L
        }
        return (value * mult).toLong()
    }

    private fun parseEta(s: String?): Long {
        if (s.isNullOrBlank()) return 0L
        val parts = s.split(":")
        return when (parts.size) {
            2 -> parts[0].toLong() * 60 + parts[1].toLong()
            3 -> parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
            else -> 0L
        }
    }

    companion object {
        private const val TAG = "YtDlpEngine"
    }
}
