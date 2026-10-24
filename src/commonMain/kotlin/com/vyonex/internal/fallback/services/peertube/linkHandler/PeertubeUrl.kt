// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler

import io.ktor.http.Url

internal object PeertubeUrl {
    internal data class Parsed(val path: String, val query: String) {
        val segments: List<String>
            get() = path.split('/').filter(String::isNotEmpty)
    }

    private val webAuthority = Regex("^https?://[^/?#]+", RegexOption.IGNORE_CASE)
    private val malformedEscape = Regex("%(?![0-9A-Fa-f]{2})")

    fun parse(value: String?): Parsed? {
        if (value.isNullOrBlank() || value.any(Char::isWhitespace) || value.contains('\\') ||
            !webAuthority.containsMatchIn(value) || value.contains(malformedEscape)
        ) return null
        return runCatching { Url(value) }
            .getOrNull()
            ?.takeIf {
                it.host.isNotBlank() && it.protocol.name.lowercase() in setOf("http", "https") &&
                    it.user == null && it.password == null
            }
            ?.let { Parsed(it.encodedPath, it.encodedQuery) }
    }

    fun segments(value: String?): List<String>? =
        parse(value)?.segments

    fun videoId(url: Parsed): String? {
        val path = url.segments
        return when {
            path.size == 2 && (path[0] == "w" || path[0] == "videos") -> path[1]
            path.size == 3 && path[0] == "videos" &&
                (path[1] == "watch" || path[1] == "embed") -> path[2]
            else -> null
        }
    }

    fun playlistId(url: Parsed): String? {
        val path = url.segments
        return when {
            path.size == 3 && path[0] == "w" && path[1] == "p" -> path[2]
            path.size == 4 && path[0] == "videos" && path[1] == "watch" &&
                path[2] == "playlist" -> path[3]
            path.size == 4 && path[0] == "api" && path[1] == "v1" &&
                path[2] == "video-playlists" -> path[3]
            else -> null
        }
    }

    fun channelId(url: Parsed): String? {
        val path = channelSegments(url)
        if (path.size <= 1) return null
        val type = when (path[0]) {
            "accounts", "a" -> "accounts"
            "video-channels", "c" -> "video-channels"
            else -> return null
        }
        return "$type/${path[1]}"
    }

    fun channelSegments(url: Parsed): List<String> {
        val path = url.segments
        return if (path.size > 2 && path[0] == "api" && path[1] == "v1") path.drop(2) else path
    }

    fun channelTabSuffix(tab: String): String? = when (tab) {
        "videos" -> "/videos"
        "channels" -> "/video-channels"
        "playlists" -> "/video-playlists"
        else -> null
    }

    fun accepts(value: String?): Boolean = parse(value) != null
}
