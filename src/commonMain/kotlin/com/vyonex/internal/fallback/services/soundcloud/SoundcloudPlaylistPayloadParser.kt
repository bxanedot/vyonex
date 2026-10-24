// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

internal data class SoundcloudPlaylistPayload(
    val playlist: SoundcloudPlaylistInfoItem,
    val description: String,
    val initialTracks: List<SoundcloudTrackInfoItem>,
    val unresolvedTrackIds: List<String>,
)

internal object SoundcloudPlaylistPayloadParser {
    fun parse(source: String): SoundcloudPlaylistPayload {
        val playlist = parseObject(source)
        val embeddedTracks = SoundcloudInfoItemPayloadParser.run { playlist.array("tracks") }
        val initialTracks = mutableListOf<SoundcloudTrackInfoItem>()
        val unresolvedTrackIds = mutableListOf<String>()
        for (value in embeddedTracks) {
            val track = SoundcloudInfoItemPayloadParser.objectValue(value) ?: continue
            SoundcloudInfoItemPayloadParser.run {
                if (track.string("title") != null) {
                    initialTracks += SoundcloudInfoItemPayloadParser.track(track)
                }
                else if (track["id"] != null && track["id"] != JsonNull) {
                    unresolvedTrackIds += track.valueAsString("id")
                }
            }
        }
        return SoundcloudPlaylistPayload(
            playlist = SoundcloudInfoItemPayloadParser.playlist(playlist),
            description = SoundcloudInfoItemPayloadParser.run {
                playlist.string("description") ?: ""
            },
            initialTracks = initialTracks.toList(),
            unresolvedTrackIds = unresolvedTrackIds.toList(),
        )
    }

    private fun parseObject(source: String): JsonObject = try {
        Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("SoundCloud playlist response must be a JSON object")
    } catch (error: SerializationException) {
        throw IllegalArgumentException("Invalid SoundCloud playlist response", error)
    }
}

internal object SoundcloudPlaylistTrackBatchParser {
    fun parse(source: String): List<SoundcloudTrackInfoItem> = parseArray(source).mapNotNull { value ->
        SoundcloudInfoItemPayloadParser.objectValue(value)?.let(SoundcloudInfoItemPayloadParser::track)
    }

    private fun parseArray(source: String): JsonArray = try {
        Json.parseToJsonElement(source) as? JsonArray
            ?: throw IllegalArgumentException("SoundCloud track lookup response must be a JSON array")
    } catch (error: SerializationException) {
        throw IllegalArgumentException("Invalid SoundCloud track lookup response", error)
    }
}
