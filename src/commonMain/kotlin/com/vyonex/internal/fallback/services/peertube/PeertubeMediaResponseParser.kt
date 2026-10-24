// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

internal data class PeertubeMediaPayload(
    val files: List<PeertubeMediaFile>,
    val playlists: List<PeertubeMediaPlaylist>,
    val isLive: Boolean,
)

internal object PeertubeMediaResponseParser {
    fun parse(source: String): PeertubeMediaPayload {
        val video = Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("Video response must be a JSON object")
        return PeertubeMediaPayload(
            files = video.array("files").mapNotNull(::mediaFile),
            playlists = video.array("streamingPlaylists").mapNotNull(::playlist),
            isLive = video.boolean("isLive"),
        )
    }

    private fun playlist(value: JsonElement): PeertubeMediaPlaylist? {
        val playlist = value as? JsonObject ?: return null
        return PeertubeMediaPlaylist(
            playlistUrl = playlist.string("playlistUrl"),
            files = playlist.array("files").mapNotNull(::mediaFile),
        )
    }

    private fun mediaFile(value: JsonElement): PeertubeMediaFile? {
        val file = value as? JsonObject ?: return null
        val resolution = file["resolution"] as? JsonObject
        return PeertubeMediaFile(
            fileUrl = file.string("fileUrl"),
            fileDownloadUrl = file.string("fileDownloadUrl"),
            torrentUrl = file.string("torrentUrl"),
            resolutionId = resolution?.number("id", -1) ?: -1,
            resolutionLabel = resolution?.string("label"),
            bitrate = file.number("bitrate", -1),
            hasAudio = file.boolean("hasAudio"),
        )
    }

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(name: String, fallback: Int): Int =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.doubleOrNull?.toInt() ?: fallback

    private fun JsonObject.boolean(name: String): Boolean =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: false
}
