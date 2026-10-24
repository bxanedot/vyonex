// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc.linkHandler

import io.ktor.http.Url
import io.ktor.http.decodeURLPart

internal object MediaCCCUrl {
    private val explicitScheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*://")
    private val malformedEscape = Regex("%(?![0-9A-Fa-f]{2})")

    internal data class Parsed(
        val scheme: String,
        val host: String,
        val path: String,
        val hasQuery: Boolean,
    )

    fun parse(value: String?): Parsed? {
        if (value.isNullOrBlank() || value.any(Char::isWhitespace)) return null
        val hasHttpScheme = value.startsWith("http://", ignoreCase = true)
            || value.startsWith("https://", ignoreCase = true)
        if (!hasHttpScheme && value.contains(explicitScheme)) return null
        if (value.contains(malformedEscape)) return null

        val candidate = if (hasHttpScheme) value else "https://$value"
        return try {
            val url = Url(candidate)
            if (url.host.isBlank() || url.protocol.name !in setOf("http", "https")) return null
            Parsed(
                scheme = url.protocol.name.lowercase(),
                host = url.host.lowercase(),
                path = url.encodedPath.decodeURLPart(),
                hasQuery = candidate.substringBefore('#').contains('?'),
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun pathSegments(uri: Parsed): List<String> = uri.path.split('/').filter(String::isNotEmpty)

    fun hasHost(uri: Parsed, host: String): Boolean = uri.host.equals(host, ignoreCase = true)
}
