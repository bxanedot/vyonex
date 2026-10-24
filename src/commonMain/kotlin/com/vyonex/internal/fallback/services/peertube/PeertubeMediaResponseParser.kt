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
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal data class PeertubeVideoMetadata(
    val name: String?,
    val publishedAt: String?,
    val description: String,
    val nsfw: Boolean,
    val duration: Long,
    val views: Long,
    val likes: Long,
    val dislikes: Long,
    val accountName: String?,
    val accountHost: String?,
    val accountDisplayName: String?,
    val channelName: String,
    val channelDisplayName: String?,
    val channelUrl: String?,
    val hlsUrl: String,
    val isLive: Boolean,
    val privacyId: Int,
    val category: String,
    val licence: String,
    val languageId: String?,
    val tags: List<String>,
    val support: String,
)

internal data class PeertubeMediaPayload(
    val files: List<PeertubeMediaFile>,
    val playlists: List<PeertubeMediaPlaylist>,
    val isLive: Boolean,
    val metadata: PeertubeVideoMetadata,
)

internal object PeertubeMediaResponseParser {
    fun parse(source: String): PeertubeMediaPayload {
        val video = Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("Video response must be a JSON object")
        return PeertubeMediaPayload(
            files = video.array("files").mapNotNull(::mediaFile),
            playlists = video.array("streamingPlaylists").mapNotNull(::playlist),
            isLive = video.boolean("isLive"),
            metadata = video.metadata(),
        )
    }

    private fun JsonObject.metadata(): PeertubeVideoMetadata {
        val account = this["account"] as? JsonObject ?: JsonObject(emptyMap())
        val channel = this["channel"] as? JsonObject ?: JsonObject(emptyMap())
        val privacy = this["privacy"] as? JsonObject ?: JsonObject(emptyMap())
        val category = this["category"] as? JsonObject ?: JsonObject(emptyMap())
        val licence = this["licence"] as? JsonObject ?: JsonObject(emptyMap())
        val language = this["language"] as? JsonObject ?: JsonObject(emptyMap())
        return PeertubeVideoMetadata(
            name = string("name"),
            publishedAt = string("publishedAt"),
            description = string("description") ?: "",
            nsfw = boolean("nsfw"),
            duration = long("duration", 0),
            views = long("views", -1),
            likes = long("likes", -1),
            dislikes = long("dislikes", -1),
            accountName = account.string("name"),
            accountHost = account.string("host"),
            accountDisplayName = account.string("displayName"),
            channelName = channel.string("name") ?: "",
            channelDisplayName = channel.string("displayName"),
            channelUrl = channel.string("url"),
            hlsUrl = array("streamingPlaylists").asSequence()
                .mapNotNull { (it as? JsonObject)?.string("playlistUrl") }
                .firstOrNull(String::isNotBlank) ?: "",
            isLive = boolean("isLive"),
            privacyId = privacy.number("id", -1),
            category = category.string("label") ?: "",
            licence = licence.string("label") ?: "",
            languageId = language.string("id"),
            tags = array("tags").mapNotNull {
                (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content
            },
            support = string("support") ?: "",
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
            ?.let { it.intOrNull ?: it.longOrNull?.toInt() ?: it.doubleOrNull?.toInt() } ?: fallback

    private fun JsonObject.long(name: String, fallback: Long): Long =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: fallback

    private fun JsonObject.boolean(name: String): Boolean =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: false
}
