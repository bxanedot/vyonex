// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object PeertubeInstanceConfigParser {
    fun parse(source: String): String? {
        val config = try {
            Json.parseToJsonElement(source) as? JsonObject ?: throw PeertubeInstanceConfigParseException()
        } catch (error: SerializationException) {
            throw PeertubeInstanceConfigParseException(error)
        }
        return ((config["instance"] as? JsonObject)?.get("name") as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
    }
}

internal class PeertubeInstanceConfigParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid PeerTube instance configuration", cause)
