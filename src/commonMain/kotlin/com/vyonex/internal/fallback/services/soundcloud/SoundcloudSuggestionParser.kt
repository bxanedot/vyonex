// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object SoundcloudSuggestionParser {
    fun parse(source: String): List<String> {
        val parsed = try {
            Json.parseToJsonElement(source)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("Invalid SoundCloud suggestions JSON", error)
        }
        val document = parsed as? JsonObject
            ?: throw IllegalArgumentException("SoundCloud suggestions must be a JSON object")
        val collection = document["collection"] as? JsonArray ?: return emptyList()
        return collection.mapNotNull { value ->
            val query = (value as? JsonObject)?.get("query") as? JsonPrimitive
            query?.takeIf { it.isString }?.content
        }
    }
}
