// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object BandcampSuggestionParser {
    fun parse(source: String): List<String> {
        val document = try {
            Json.parseToJsonElement(source) as? JsonObject
        } catch (_: SerializationException) {
            null
        } ?: return emptyList()
        val results = ((document["auto"] as? JsonObject)?.get("results") as? JsonArray)
            ?: return emptyList()
        val names = linkedSetOf<String>()
        for (item in results) {
            val name = (item as? JsonObject)?.get("name") as? JsonPrimitive ?: continue
            if (name.isString && name.content.isNotBlank()) names.add(name.content)
        }
        return names.toList()
    }
}
