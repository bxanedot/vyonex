// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler

import io.ktor.http.Url

internal object PeertubeUrl {
    internal data class Parsed(val path: String, val query: String)

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
        parse(value)?.path?.split('/')?.filter(String::isNotEmpty)

    fun accepts(value: String?): Boolean = parse(value) != null
}
