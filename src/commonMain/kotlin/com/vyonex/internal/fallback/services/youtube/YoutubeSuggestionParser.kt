// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

internal object YoutubeSuggestionParser {
    fun parse(source: String): List<String> {
        val response = try {
            Json.parseToJsonElement(source) as? JsonArray ?: throw YoutubeSuggestionParseException()
        } catch (error: SerializationException) {
            throw YoutubeSuggestionParseException(error)
        }
        val rows = response.getOrNull(1) as? JsonArray ?: throw YoutubeSuggestionParseException()
        return rows.mapNotNull { value ->
            val suggestion = ((value as? JsonArray)?.firstOrNull() as? JsonPrimitive)
                ?.takeIf { it.isString }?.content
            suggestion?.takeIf { it.isNotBlank() }
        }
    }
}

internal class YoutubeSuggestionParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid YouTube suggestion data", cause)
