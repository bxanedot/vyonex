// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal data class BandcampPlaylistTrackPayload(
    val title: String?,
    val titleLink: String?,
    val duration: Long,
    val artist: String,
)

internal data class BandcampPlaylistTracksPayload(
    val url: String?,
    val artist: String,
    val artId: Long,
    val trackCount: Int,
    val tracks: List<BandcampPlaylistTrackPayload>,
)

internal object BandcampPlaylistPageParser {
    fun parseTracks(source: String): BandcampPlaylistTracksPayload {
        val album = parseObject(source)
        val tracks = album.array("trackinfo")
        return BandcampPlaylistTracksPayload(
            url = album.string("url"),
            artist = album.string("artist") ?: "",
            artId = album.long("art_id", 0),
            trackCount = tracks.size,
            tracks = tracks.mapNotNull(::track),
        )
    }

    fun parseTitle(source: String): String? = parseObject(source).string("album_title")

    private fun parseObject(source: String): JsonObject = try {
        Json.parseToJsonElement(source) as? JsonObject ?: throw BandcampPlaylistPageParseException()
    } catch (error: SerializationException) {
        throw BandcampPlaylistPageParseException(error)
    }

    private fun track(value: JsonElement): BandcampPlaylistTrackPayload? {
        val track = value as? JsonObject ?: return null
        return BandcampPlaylistTrackPayload(
            title = track.string("title"),
            titleLink = track.string("title_link"),
            duration = track.double("duration", 0.0).toLong(),
            artist = track.string("artist") ?: "",
        )
    }

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.long(name: String, fallback: Long): Long =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: fallback

    private fun JsonObject.double(name: String, fallback: Double): Double =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.doubleOrNull ?: fallback
}

internal class BandcampPlaylistPageParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid Bandcamp playlist data", cause)
