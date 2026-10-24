// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal object PeertubeAccountChannelsParser {
    fun followerCount(source: String): Long {
        val response = try {
            Json.parseToJsonElement(source) as? JsonObject ?: throw PeertubeAccountChannelsParseException()
        } catch (error: SerializationException) {
            throw PeertubeAccountChannelsParseException(error)
        }
        val channels = response["data"] as? JsonArray ?: throw PeertubeAccountChannelsParseException()
        return channels.sumOf { channel ->
            (channel as? JsonObject)?.long("followersCount") ?: 0
        }
    }

    private fun JsonObject.long(name: String): Long =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: 0
}

internal class PeertubeAccountChannelsParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid PeerTube account channel data", cause)
