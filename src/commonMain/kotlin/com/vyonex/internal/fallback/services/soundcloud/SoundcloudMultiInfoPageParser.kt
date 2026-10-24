// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal data class SoundcloudMultiInfoPagePayload(
    val items: List<SoundcloudSearchResult>,
    val nextPageUrl: String?,
)

internal object SoundcloudMultiInfoPageParser {
    fun parse(source: String): SoundcloudMultiInfoPagePayload {
        val response = parseObject(source)
        return SoundcloudMultiInfoPagePayload(
            items = SoundcloudInfoItemPayloadParser.run {
                response.array("collection").mapNotNull { value ->
                    val item = objectValue(value) ?: return@mapNotNull null
                    SoundcloudSearchPageParser.result(item) ?: likedItem(item)
                }
            },
            nextPageUrl = SoundcloudInfoItemPayloadParser.run { response.string("next_href") },
        )
    }

    private fun likedItem(item: JsonObject): SoundcloudSearchResult? =
        if (SoundcloudInfoItemPayloadParser.run { item.string("kind") } != "like") null
        else {
            val playlist = item["playlist"] as? JsonObject
            if (playlist != null) {
                SoundcloudSearchResult.Playlist(SoundcloudInfoItemPayloadParser.playlist(playlist))
            } else {
                SoundcloudSearchResult.Track(
                    SoundcloudInfoItemPayloadParser.run { item.objectValue("track") }
                        .let(SoundcloudInfoItemPayloadParser::track),
                )
            }
        }

    private fun parseObject(source: String): JsonObject = try {
        Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("SoundCloud items response must be a JSON object")
    } catch (error: SerializationException) {
        throw IllegalArgumentException("Invalid SoundCloud items response", error)
    }
}
