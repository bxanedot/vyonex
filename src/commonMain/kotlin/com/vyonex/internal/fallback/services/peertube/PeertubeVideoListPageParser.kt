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

internal data class PeertubeVideoListPagePayload(
    val total: Long,
    val data: JsonArray?,
    val error: String?,
)

internal object PeertubeVideoListPageParser {
    fun parse(source: String): PeertubeVideoListPagePayload {
        val response = try {
            Json.parseToJsonElement(source) as? JsonObject ?: throw PeertubeVideoListPageParseException()
        } catch (error: SerializationException) {
            throw PeertubeVideoListPageParseException(error)
        }
        return PeertubeVideoListPagePayload(
            total = response.long("total"),
            data = response["data"] as? JsonArray,
            error = response.string("error")?.takeIf(String::isNotBlank),
        )
    }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.long(name: String): Long =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: 0
}

internal class PeertubeVideoListPageParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid PeerTube video list response", cause)
