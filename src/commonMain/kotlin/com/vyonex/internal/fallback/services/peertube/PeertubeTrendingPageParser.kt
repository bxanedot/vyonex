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

internal data class PeertubeTrendingPagePayload(
    val total: Long,
    val data: JsonArray?,
    val error: String?,
)

internal object PeertubeTrendingPageParser {
    fun parse(source: String): PeertubeTrendingPagePayload {
        val response = try {
            Json.parseToJsonElement(source) as? JsonObject ?: throw PeertubeTrendingPageParseException()
        } catch (error: SerializationException) {
            throw PeertubeTrendingPageParseException(error)
        }
        return PeertubeTrendingPagePayload(
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

internal class PeertubeTrendingPageParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid PeerTube kiosk results", cause)
