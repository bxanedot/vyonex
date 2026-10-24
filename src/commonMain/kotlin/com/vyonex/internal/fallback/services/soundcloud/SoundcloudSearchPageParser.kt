// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal sealed interface SoundcloudSearchResult {
    data class Track(val item: SoundcloudTrackInfoItem) : SoundcloudSearchResult
    data class User(val item: SoundcloudProfileInfoItem) : SoundcloudSearchResult
    data class Playlist(val item: SoundcloudPlaylistInfoItem) : SoundcloudSearchResult
}

internal data class SoundcloudSearchPagePayload(
    val results: List<SoundcloudSearchResult>,
    val collectionSize: Int,
    val nextPageUrl: String?,
)

internal object SoundcloudSearchPageParser {
    fun parse(source: String): SoundcloudSearchPagePayload {
        val document = parseObject(source)
        val collection = SoundcloudInfoItemPayloadParser.run { document.array("collection") }
        return SoundcloudSearchPagePayload(
            results = collection.mapNotNull { value ->
                SoundcloudInfoItemPayloadParser.objectValue(value)?.let(::result)
            },
            collectionSize = collection.size,
            nextPageUrl = SoundcloudInfoItemPayloadParser.run { document.string("next_href") },
        )
    }

    internal fun result(item: JsonObject): SoundcloudSearchResult? =
        when (SoundcloudInfoItemPayloadParser.run { item.string("kind") }) {
            "track" -> SoundcloudSearchResult.Track(SoundcloudInfoItemPayloadParser.track(item))
            "user" -> SoundcloudSearchResult.User(SoundcloudInfoItemPayloadParser.profile(item))
            "playlist" -> SoundcloudSearchResult.Playlist(
                SoundcloudInfoItemPayloadParser.playlist(item),
            )
            else -> null
        }

    private fun parseObject(source: String): JsonObject = try {
        Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("SoundCloud search response must be a JSON object")
    } catch (error: SerializationException) {
        throw IllegalArgumentException("Invalid SoundCloud search response", error)
    }
}
