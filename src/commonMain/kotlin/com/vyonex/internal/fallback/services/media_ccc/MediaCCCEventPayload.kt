// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc

import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class MediaCCCEventPayload(
    val title: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val description: String? = null,
    val length: Long? = null,
    @SerialName("view_count") val viewCount: Long? = null,
    @SerialName("conference_url") val conferenceUrl: String? = null,
    @SerialName("frontend_link") val frontendLink: String? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    val tags: List<JsonElement>? = null,
    val recordings: List<MediaCCCRecordingPayload>? = null,
    @SerialName("thumb_url") val thumbUrl: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
) {
    fun stringTags(): List<String> = tags.orEmpty()
        .filterIsInstance<JsonPrimitive>()
        .filter { it.isString }
        .map { it.content }

    companion object {
        private val decoder = Json { ignoreUnknownKeys = true }

        fun decode(body: String): MediaCCCEventPayload = try {
            decoder.decodeFromString(body)
        } catch (error: SerializationException) {
            throw MediaCCCEventParseException(error)
        }
    }
}

@Serializable
internal data class MediaCCCRecordingPayload(
    val filename: String? = null,
    @SerialName("recording_url") val recordingUrl: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    val language: String? = null,
    val height: Int? = null,
)

internal class MediaCCCEventParseException(cause: SerializationException) :
    IllegalArgumentException("Invalid MediaCCC event JSON", cause)
