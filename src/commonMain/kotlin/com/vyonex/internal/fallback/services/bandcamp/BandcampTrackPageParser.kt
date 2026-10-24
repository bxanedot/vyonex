// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal data class BandcampTrackPagePayload(
    val trackCount: Int,
    val hasFile: Boolean,
    val title: String?,
    val url: String?,
    val artist: String?,
    val publishDate: String?,
    val artId: Long,
    val description: String,
    val audioUrl: String?,
    val durationSeconds: Double,
)

internal object BandcampTrackPageParser {
    fun parse(source: String): BandcampTrackPagePayload {
        val root = try {
            Json.parseToJsonElement(source) as? JsonObject ?: throw BandcampTrackPageParseException()
        } catch (error: SerializationException) {
            throw BandcampTrackPageParseException(error)
        }
        val tracks = root.array("trackinfo")
        val track = tracks.firstOrNull() as? JsonObject ?: JsonObject(emptyMap())
        val current = root["current"] as? JsonObject ?: JsonObject(emptyMap())
        val fileValue = track["file"]
        val file = fileValue as? JsonObject
        return BandcampTrackPagePayload(
            trackCount = tracks.size,
            hasFile = fileValue != null && fileValue != JsonNull,
            title = current.string("title"),
            url = root.string("url"),
            artist = root.string("artist"),
            publishDate = current.string("publish_date"),
            artId = root.long("art_id", 0),
            description = listOf("about", "lyrics", "credits")
                .mapNotNull { current.string(it) }
                .filter(String::isNotBlank)
                .joinToString("\n\n"),
            audioUrl = file?.string("mp3-128")?.takeIf(String::isNotBlank),
            durationSeconds = track.double("duration", 0.0),
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

internal class BandcampTrackPageParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid Bandcamp track page data", cause)
