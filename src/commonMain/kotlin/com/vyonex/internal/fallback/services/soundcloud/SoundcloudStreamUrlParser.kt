// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.soundcloud

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object SoundcloudStreamUrlParser {
    fun parse(source: String): String? = try {
        val response = Json.decodeFromString<JsonObject>(source)
        (response["url"] as? JsonPrimitive)?.takeIf { it.isString }?.content
    } catch (error: SerializationException) {
        throw SoundcloudStreamUrlParseException(error)
    }
}

internal class SoundcloudStreamUrlParseException(cause: SerializationException) :
    IllegalArgumentException("Invalid SoundCloud stream URL response", cause)
