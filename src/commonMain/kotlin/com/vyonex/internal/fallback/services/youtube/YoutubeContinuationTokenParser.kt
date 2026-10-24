// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object YoutubeContinuationTokenParser {
    fun parse(source: String): String? {
        val renderer = try {
            Json.parseToJsonElement(source) as? JsonObject
        } catch (_: SerializationException) {
            null
        } ?: return null
        val endpoint = renderer["continuationEndpoint"] as? JsonObject ?: return null
        val command = endpoint["continuationCommand"] as? JsonObject ?: return null
        return (command["token"] as? JsonPrimitive)?.takeIf { it.isString }
            ?.content?.takeIf(String::isNotEmpty)
    }
}
