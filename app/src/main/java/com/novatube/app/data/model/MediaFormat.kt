package com.novatube.app.data.model

import com.google.gson.annotations.SerializedName

/** Subset of a yt-dlp format entry, normalized for UI display. */
data class MediaFormat(
    @SerializedName("format_id") val formatId: String? = null,
    @SerializedName("format_note") val formatNote: String? = null,
    @SerializedName("ext") val ext: String? = null,
    @SerializedName("acodec") val acodec: String? = null,
    @SerializedName("vcodec") val vcodec: String? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("tbr") val tbr: Double? = null,
    @SerializedName("abr") val abr: Double? = null,
    @SerializedName("vbr") val vbr: Double? = null,
    @SerializedName("fps") val fps: Double? = null,
    @SerializedName("filesize") val filesize: Long? = null,
    @SerializedName("filesize_approx") val filesizeApprox: Long? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("manifest_url") val manifestUrl: String? = null,
    @SerializedName("protocol") val protocol: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("dynamic_range") val dynamicRange: String? = null,
    @SerializedName("container") val container: String? = null,
    @SerializedName("asr") val asr: Int? = null
) {
    val isVideo: Boolean
        get() = !vcodec.isNullOrEmpty() && vcodec != "none" && height != null

    val isAudio: Boolean
        get() = (!acodec.isNullOrEmpty() && acodec != "none") &&
            (vcodec.isNullOrEmpty() || vcodec == "none")

    val displayResolution: String
        get() = when {
            height != null && fps != null && fps > 30 -> "${height}p${fps.toInt()}"
            height != null -> "${height}p"
            vbr != null -> "${vbr.toInt()}k"
            tbr != null -> "${tbr.toInt()}k"
            else -> formatNote ?: formatId ?: "—"
        }

    val displayExt: String get() = ext?.uppercase() ?: "?"
    val displaySize: String? get() = (filesize ?: filesizeApprox)?.let { humanReadableSize(it) }
    val displayBitrate: String
        get() = when {
            abr != null && vbr != null -> "${abr.toInt()}k audio / ${vbr.toInt()}k video"
            abr != null -> "${abr.toInt()}kbps"
            vbr != null -> "${vbr.toInt()}kbps"
            tbr != null -> "${tbr.toInt()}kbps"
            else -> "—"
        }
}

fun humanReadableSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024 && i < units.lastIndex) {
        v /= 1024
        i++
    }
    return String.format("%.1f %s", v, units[i])
}

fun formatSpeed(bytesPerSec: Long): String = humanReadableSize(bytesPerSec) + "/s"

fun formatEta(seconds: Long): String {
    if (seconds <= 0) return "—"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}
