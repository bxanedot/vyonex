// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

internal data class BandcampArtistDetailsPayload(
    val hasError: Boolean,
    val name: String?,
    val url: String?,
    val bio: String,
    val bioImageId: Long,
    val discography: List<BandcampDiscographyItem>,
)

internal data class BandcampDiscographyItem(
    val type: String?,
    val title: String?,
    val artist: String,
    val artistId: Long,
    val itemId: Long,
    val artId: Long,
)

internal object BandcampArtistDetailsParser {
    fun parse(source: String): BandcampArtistDetailsPayload {
        val artist = parseObject(source)
        return BandcampArtistDetailsPayload(
            hasError = artist.boolean("error"),
            name = artist.string("name"),
            url = artist.string("bandcamp_url"),
            bio = artist.string("bio") ?: "",
            bioImageId = artist.long("bio_image_id"),
            discography = artist.array("discography").mapNotNull(::discographyItem),
        )
    }

    fun parseBandId(source: String): Long = parseObject(source).long("id")

    private fun parseObject(source: String): JsonObject = try {
        Json.parseToJsonElement(source) as? JsonObject ?: throw BandcampArtistDetailsParseException()
    } catch (error: SerializationException) {
        throw BandcampArtistDetailsParseException(error)
    }

    private fun discographyItem(value: JsonElement): BandcampDiscographyItem? {
        val item = value as? JsonObject ?: return null
        return BandcampDiscographyItem(
            type = item.string("item_type"),
            title = item.string("title"),
            artist = item.string("band_name") ?: "",
            artistId = item.long("band_id"),
            itemId = item.long("item_id"),
            artId = item.long("art_id"),
        )
    }

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content

    private fun JsonObject.long(name: String): Long =
        (this[name] as? JsonPrimitive)?.takeUnless(JsonPrimitive::isString)?.longOrNull ?: 0

    private fun JsonObject.boolean(name: String): Boolean =
        (this[name] as? JsonPrimitive)?.takeUnless(JsonPrimitive::isString)?.booleanOrNull ?: false
}

internal class BandcampArtistDetailsParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid Bandcamp artist details", cause)
