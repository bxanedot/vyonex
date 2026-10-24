// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class MediaCCCLiveConferencePayload(
    val conference: String? = null,
    val slug: String? = null,
    val description: String? = null,
    val isCurrentlyStreaming: Boolean? = null,
    val groups: List<MediaCCCLiveGroupPayload>? = null,
)

@Serializable
internal data class MediaCCCLiveGroupPayload(
    val group: String? = null,
    val rooms: List<MediaCCCLiveRoomPayload>? = null,
)

@Serializable
internal data class MediaCCCLiveRoomPayload(
    val slug: String? = null,
    val display: String? = null,
    val link: String? = null,
    val thumb: String? = null,
    val poster: String? = null,
    val talks: MediaCCCLiveTalksPayload? = null,
    val streams: List<MediaCCCLiveStreamPayload>? = null,
)

@Serializable
internal data class MediaCCCLiveTalksPayload(
    val current: MediaCCCLiveTalkPayload? = null,
    val next: MediaCCCLiveTalkPayload? = null,
)

@Serializable
internal data class MediaCCCLiveTalkPayload(val title: String? = null)

@Serializable
internal data class MediaCCCLiveStreamPayload(
    val type: String? = null,
    val videoSize: List<Int?>? = null,
    val urls: Map<String, JsonElement>? = null,
) {
    fun sources(): List<MediaCCCLiveSourcePayload> = urls.orEmpty().mapNotNull { (format, value) ->
        val fields = value as? JsonObject ?: return@mapNotNull null
        MediaCCCLiveSourcePayload(format, fields["tech"].stringValue(), fields["url"].stringValue())
    }
}

internal data class MediaCCCLiveSourcePayload(
    val format: String,
    val tech: String?,
    val url: String?,
)

internal object MediaCCCLivePayloadDecoder {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(body: String): List<MediaCCCLiveConferencePayload> = try {
        json.decodeFromString(body)
    } catch (error: SerializationException) {
        throw MediaCCCLivePayloadParseException(error)
    }
}

internal class MediaCCCLivePayloadParseException(cause: SerializationException) :
    IllegalArgumentException("Invalid MediaCCC live stream JSON", cause)

private fun JsonElement?.stringValue(): String? =
    (this as? JsonPrimitive)?.takeIf { it.isString }?.content
