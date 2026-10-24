// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal data class BandcampRadioEpisodePayload(
    val subtitle: String?,
    val imageCaption: String?,
    val publishedDate: String?,
    val imageId: Long,
    val description: String?,
    val duration: Long,
    val mp3Url: String?,
    val opusUrl: String?,
    val segments: List<BandcampRadioEpisodeSegment>,
)

internal data class BandcampRadioEpisodeSegment(
    val title: String,
    val timecode: Int,
    val artId: Long,
    val artist: String?,
)

internal object BandcampRadioEpisodeParser {
    fun parse(source: String): BandcampRadioEpisodePayload {
        val data = try {
            Json.parseToJsonElement(source) as? JsonObject ?: throw BandcampRadioEpisodeParseException()
        } catch (error: SerializationException) {
            throw BandcampRadioEpisodeParseException(error)
        }
        val audio = data["audio_stream"] as? JsonObject
        val segments = (data["tracks"] as? JsonArray).orEmpty().mapNotNull { value ->
            val track = value as? JsonObject ?: return@mapNotNull null
            val title = track.string("title")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            BandcampRadioEpisodeSegment(
                title = title,
                timecode = track.int("timecode"),
                artId = track.long("track_art_id"),
                artist = track.string("artist"),
            )
        }
        return BandcampRadioEpisodePayload(
            subtitle = data.string("subtitle"),
            imageCaption = data.string("image_caption"),
            publishedDate = data.string("published_date"),
            imageId = data.long("show_image_id"),
            description = data.string("desc"),
            duration = data.long("audio_duration"),
            mp3Url = audio?.string("mp3-128"),
            opusUrl = audio?.string("opus-lo"),
            segments = segments,
        )
    }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.long(name: String): Long =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: 0

    private fun JsonObject.int(name: String): Int =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.intOrNull ?: it.doubleOrNull?.toInt() } ?: 0
}

internal class BandcampRadioEpisodeParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid Bandcamp radio-show data", cause)
