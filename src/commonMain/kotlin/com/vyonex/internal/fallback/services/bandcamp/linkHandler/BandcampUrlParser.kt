// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

import com.vyonex.internal.url.FormUrlEncoder
import io.ktor.http.Url
import io.ktor.http.decodeURLPart

internal object BandcampUrlParser {
    internal const val BASE_URL = "https://bandcamp.com"

    internal data class Parsed(
        val host: String,
        val path: String,
        val rawQuery: String?,
    ) {
        val segments: List<String>
            get() = path.split('/').filter(String::isNotEmpty)
    }

    private val webAuthority = Regex("^https?://[^/?#]+", RegexOption.IGNORE_CASE)
    private val malformedEscape = Regex("%(?![0-9A-Fa-f]{2})")

    fun parse(value: String?): Parsed? {
        if (value.isNullOrBlank() || value.any(Char::isWhitespace) || value.contains('\\') ||
            !webAuthority.containsMatchIn(value) || value.contains(malformedEscape)
        ) return null

        val url = runCatching { Url(value) }.getOrNull()
            ?.takeIf { it.host.isNotBlank() && it.protocol.name.lowercase() in setOf("http", "https") }
            ?: return null
        val fragment = value.indexOf('#').takeIf { it >= 0 } ?: value.length
        val query = value.indexOf('?').takeIf { it >= 0 && it < fragment }
            ?.let { value.substring(it + 1, fragment) }
        return runCatching {
            Parsed(url.host, url.encodedPath.decodeURLPart(), query)
        }.getOrNull()
    }

    fun isArtistPage(url: Parsed): Boolean {
        if (url.host.equals("daily.bandcamp.com", ignoreCase = true)) return false
        val segments = url.segments
        return segments.size <= 1 && (segments.isEmpty() ||
            segments[0].lowercase() in setOf("releases", "music", "album", "track"))
    }

    fun isArtistContent(url: Parsed, kind: String): Boolean {
        val path = url.path.lowercase()
        val prefix = "/$kind/"
        return path.startsWith(prefix) && path.length > prefix.length
    }

    fun searchUrl(query: String): String = "$BASE_URL/search?q=${FormUrlEncoder.encode(query)}&page=1"
}
