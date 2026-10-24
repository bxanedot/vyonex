// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal data class PeertubePlaylistPagePayload(
    val displayName: String?,
    val description: String?,
    val videoCount: Long,
    val ownerName: String?,
    val ownerUrl: String?,
    val channelName: String?,
    val channelUrl: String?,
    val error: String?,
    val thumbnails: List<PeertubeImageReference>,
    val ownerAvatars: List<PeertubeImageReference>,
    val channelAvatars: List<PeertubeImageReference>,
)

internal object PeertubePlaylistPageParser {
    fun parse(source: String): PeertubePlaylistPagePayload {
        val response = try {
            Json.parseToJsonElement(source) as? JsonObject ?: throw PeertubePlaylistPageParseException()
        } catch (error: SerializationException) {
            throw PeertubePlaylistPageParseException(error)
        }
        val error = response.string("error")?.takeIf(String::isNotBlank)
        val owner = response["ownerAccount"] as? JsonObject
            ?: if (error == null) throw PeertubePlaylistPageParseException() else JsonObject(emptyMap())
        val channel = response["videoChannel"] as? JsonObject
            ?: if (error == null) throw PeertubePlaylistPageParseException() else JsonObject(emptyMap())
        return PeertubePlaylistPagePayload(
            displayName = response.string("displayName"),
            description = response.string("description"),
            videoCount = response.long("videosLength", -1),
            ownerName = owner.string("displayName"),
            ownerUrl = owner.string("url"),
            channelName = channel.string("displayName"),
            channelUrl = channel.string("url"),
            error = error,
            thumbnails = PeertubeImageParser.thumbnails(
                response.string("thumbnailPath"), response.string("previewPath"),
            ),
            ownerAvatars = PeertubeImageParser.profileImages(owner, "avatars", "avatar"),
            channelAvatars = PeertubeImageParser.profileImages(channel, "avatars", "avatar"),
        )
    }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.long(name: String, fallback: Long): Long =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: fallback
}

internal class PeertubePlaylistPageParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid PeerTube playlist data", cause)
