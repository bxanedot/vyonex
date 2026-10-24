// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal data class BandcampRadioShow(
    val id: Long,
    val subtitle: String?,
    val title: String,
    val description: String?,
    val date: String?,
    val imageId: Long,
)

internal object BandcampRadioPageParser {
    fun parse(source: String): List<BandcampRadioShow> {
        val root = try {
            Json.parseToJsonElement(source) as? JsonObject ?: throw BandcampRadioPageParseException()
        } catch (error: SerializationException) {
            throw BandcampRadioPageParseException(error)
        }
        val results = root["results"] as? JsonArray ?: throw BandcampRadioResultsMissingException()
        return results.mapNotNull { value ->
            val show = value as? JsonObject ?: return@mapNotNull null
            BandcampRadioShow(
                id = show.long("id") ?: 0,
                subtitle = show.string("subtitle"),
                title = show.string("title") ?: "",
                description = show.string("desc"),
                date = show.string("date"),
                imageId = show.long("image_id") ?: 0,
            )
        }
    }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.long(name: String): Long? =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }
}

internal class BandcampRadioPageParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid Bandcamp radio data", cause)

internal class BandcampRadioResultsMissingException :
    IllegalArgumentException("Bandcamp radio data has no results")
