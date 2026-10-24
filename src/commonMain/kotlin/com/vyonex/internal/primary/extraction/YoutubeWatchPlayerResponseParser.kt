// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import com.vyonex.internal.primary.models.response.PlayerResponse

internal object YoutubeWatchPlayerResponseParser {
    fun parse(html: String): PlayerResponse? {
        var searchFrom = 0
        while (true) {
            val marker = html.indexOf(PLAYER_RESPONSE, searchFrom)
            if (marker < 0) return null
            searchFrom = marker + PLAYER_RESPONSE.length

            val start = assignedObjectStart(html, searchFrom) ?: continue
            val end = objectEnd(html, start) ?: continue
            val response = runCatching { PlayerResponse.parse(html.substring(start, end)) }.getOrNull()
            if (response != null) return response
        }
    }

    private fun assignedObjectStart(source: String, afterMarker: Int): Int? {
        var cursor = source.skipWhitespace(afterMarker)
        if (source.getOrNull(cursor) == '"' || source.getOrNull(cursor) == '\'') cursor++
        cursor = source.skipWhitespace(cursor)
        if (source.getOrNull(cursor) == ']') cursor++
        cursor = source.skipWhitespace(cursor)
        if (source.getOrNull(cursor) != '=') return null
        cursor = source.skipWhitespace(cursor + 1)
        return cursor.takeIf { source.getOrNull(it) == '{' }
    }

    private fun String.skipWhitespace(from: Int): Int {
        var cursor = from
        while (getOrNull(cursor)?.isWhitespace() == true) cursor++
        return cursor
    }

    private fun objectEnd(source: String, start: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until source.length) {
            val char = source[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
            } else {
                when (char) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> if (--depth == 0) return index + 1
                }
            }
        }
        return null
    }

    private const val PLAYER_RESPONSE = "ytInitialPlayerResponse"
}
