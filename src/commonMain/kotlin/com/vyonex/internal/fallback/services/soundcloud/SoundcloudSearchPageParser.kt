// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal data class SoundcloudSearchProfile(
    val name: String,
    val permalinkUrl: String,
    val avatarUrl: String?,
    val verified: Boolean,
    val followers: Long,
    val tracks: Long,
    val description: String,
)

internal sealed interface SoundcloudSearchResult {
    data class Track(
        val permalinkUrl: String,
        val title: String,
        val durationMillis: Long,
        val user: SoundcloudSearchProfile,
        val createdAt: String,
        val playbackCount: Long,
        val artworkUrl: String?,
    ) : SoundcloudSearchResult

    data class User(val profile: SoundcloudSearchProfile) : SoundcloudSearchResult

    data class Playlist(
        val title: String,
        val permalinkUrl: String,
        val user: SoundcloudSearchProfile,
        val trackCount: Long,
        val artworkUrl: String?,
    ) : SoundcloudSearchResult
}

internal data class SoundcloudSearchPagePayload(
    val results: List<SoundcloudSearchResult>,
    val collectionSize: Int,
    val nextPageUrl: String?,
)

internal object SoundcloudSearchPageParser {
    fun parse(source: String): SoundcloudSearchPagePayload {
        val document = try {
            Json.parseToJsonElement(source) as? JsonObject
                ?: throw IllegalArgumentException("SoundCloud search response must be a JSON object")
        } catch (error: SerializationException) {
            throw IllegalArgumentException("Invalid SoundCloud search response", error)
        }
        val collection = document.array("collection")
        return SoundcloudSearchPagePayload(
            results = collection.mapNotNull(::result),
            collectionSize = collection.size,
            nextPageUrl = document.string("next_href"),
        )
    }

    private fun result(value: JsonElement): SoundcloudSearchResult? {
        val item = value as? JsonObject ?: return null
        return when (item.string("kind")) {
            "track" -> {
                val user = item.objectValue("user").profile()
                SoundcloudSearchResult.Track(
                    permalinkUrl = item.string("permalink_url") ?: "",
                    title = item.string("title") ?: "",
                    durationMillis = item.long("duration"),
                    user = user,
                    createdAt = item.string("created_at") ?: "",
                    playbackCount = item.long("playback_count"),
                    artworkUrl = item.string("artwork_url") ?: user.avatarUrl,
                )
            }
            "user" -> SoundcloudSearchResult.User(item.profile())
            "playlist" -> {
                val user = item.objectValue("user").profile()
                SoundcloudSearchResult.Playlist(
                    title = item.string("title") ?: "",
                    permalinkUrl = item.string("permalink_url") ?: "",
                    user = user,
                    trackCount = item.long("track_count"),
                    artworkUrl = item.string("artwork_url")
                        ?.takeIf(String::isNotBlank)
                        ?: item.array("tracks").firstNotNullOfOrNull(::trackArtwork)
                        ?: user.avatarUrl,
                )
            }
            else -> null
        }
    }

    private fun trackArtwork(value: JsonElement): String? {
        val track = value as? JsonObject ?: return null
        return track.string("artwork_url")?.takeIf(String::isNotBlank)
            ?: track.objectValue("user").string("avatar_url")?.takeIf(String::isNotBlank)
    }

    private fun JsonObject.profile() = SoundcloudSearchProfile(
        name = string("username") ?: "",
        permalinkUrl = string("permalink_url") ?: "",
        avatarUrl = string("avatar_url"),
        verified = boolean("verified"),
        followers = long("followers_count"),
        tracks = long("track_count"),
        description = string("description") ?: "",
    )

    private fun JsonObject.objectValue(name: String): JsonObject =
        this[name] as? JsonObject ?: JsonObject(emptyMap())

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.long(name: String): Long =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: 0

    private fun JsonObject.boolean(name: String): Boolean =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: false
}
