package com.novatube.app.nav

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Route(val path: String) {
    object Home : Route("home")
    object Search : Route("search?query={query}") {
        const val ARG_QUERY = "query"
        fun withQuery(q: String? = null): String = if (q.isNullOrBlank()) "search" else "search?query=${enc(q)}"
    }
    object Downloads : Route("downloads")
    object Library : Route("library")
    object Player : Route("player?url={url}&title={title}&type={type}") {
        const val ARG_URL = "url"
        const val ARG_TITLE = "title"
        const val ARG_TYPE = "type" // local|remote
        fun build(url: String, title: String = "", type: String = "local"): String =
            "player?url=${enc(url)}&title=${enc(title)}&type=${type}"
    }
    object Format : Route("format?url={url}&source={source}") {
        const val ARG_URL = "url"
        const val ARG_SOURCE = "source" // direct|share
        fun build(url: String, source: String = "direct"): String =
            "format?url=${enc(url)}&source=${source}"
    }
    object Settings : Route("settings")
    object Browser : Route("browser?url={url}") {
        const val ARG_URL = "url"
        fun with(url: String? = null): String = if (url.isNullOrBlank()) "browser" else "browser?url=${enc(url)}"
    }
    object Music : Route("music")
    object Playlists : Route("playlists")
    object History : Route("history")
    object Splash : Route("splash")
    object Onboarding : Route("onboarding")

    private companion object {
        fun enc(v: String): String = URLEncoder.encode(v, StandardCharsets.UTF_8.name())
    }
}
