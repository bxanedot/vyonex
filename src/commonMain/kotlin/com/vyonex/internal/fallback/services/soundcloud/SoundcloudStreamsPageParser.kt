// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal data class SoundcloudStreamsPagePayload(
    val tracks: List<SoundcloudTrackInfoItem>,
    val nextPageUrl: String?,
)

internal object SoundcloudStreamsPageParser {
    fun parse(source: String, charts: Boolean = false): SoundcloudStreamsPagePayload {
        val response = parseObject(source)
        return SoundcloudStreamsPagePayload(
            tracks = SoundcloudInfoItemPayloadParser.run {
                response.array("collection").mapNotNull { value ->
                    val item = objectValue(value) ?: return@mapNotNull null
                    track(if (charts) item.objectValue("track") else item)
                }
            },
            nextPageUrl = SoundcloudInfoItemPayloadParser.run { response.string("next_href") },
        )
    }

    private fun parseObject(source: String): JsonObject = try {
        Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("SoundCloud tracks response must be a JSON object")
    } catch (error: SerializationException) {
        throw IllegalArgumentException("Invalid SoundCloud tracks response", error)
    }
}
