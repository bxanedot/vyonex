// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal data class SoundcloudProfileInfoItem(
    val name: String,
    val permalinkUrl: String,
    val avatarUrl: String?,
    val verified: Boolean,
    val followers: Long,
    val trackCount: Long,
    val description: String,
)

internal data class SoundcloudTrackInfoItem(
    val id: String,
    val permalinkUrl: String,
    val title: String,
    val durationMillis: Long,
    val user: SoundcloudProfileInfoItem,
    val createdAt: String,
    val playbackCount: Long,
    val artworkUrl: String?,
)

internal data class SoundcloudPlaylistInfoItem(
    val title: String,
    val permalinkUrl: String,
    val user: SoundcloudProfileInfoItem,
    val trackCount: Long,
    val artworkUrl: String?,
)

internal object SoundcloudInfoItemPayloadParser {
    fun profile(source: JsonObject): SoundcloudProfileInfoItem = SoundcloudProfileInfoItem(
        name = source.string("username") ?: "",
        permalinkUrl = source.string("permalink_url") ?: "",
        avatarUrl = source.string("avatar_url"),
        verified = source.boolean("verified"),
        followers = source.long("followers_count"),
        trackCount = source.long("track_count"),
        description = source.string("description") ?: "",
    )

    fun track(source: JsonObject): SoundcloudTrackInfoItem {
        val user = profile(source.objectValue("user"))
        return SoundcloudTrackInfoItem(
            id = source.valueAsString("id"),
            permalinkUrl = source.string("permalink_url") ?: "",
            title = source.string("title") ?: "",
            durationMillis = source.long("duration"),
            user = user,
            createdAt = source.string("created_at") ?: "",
            playbackCount = source.long("playback_count"),
            artworkUrl = source.string("artwork_url") ?: user.avatarUrl,
        )
    }

    fun playlist(source: JsonObject): SoundcloudPlaylistInfoItem {
        val user = profile(source.objectValue("user"))
        return SoundcloudPlaylistInfoItem(
            title = source.string("title") ?: "",
            permalinkUrl = source.string("permalink_url") ?: "",
            user = user,
            trackCount = source.long("track_count"),
            artworkUrl = source.string("artwork_url")?.takeIf(String::isNotBlank)
                ?: source.array("tracks").firstNotNullOfOrNull(::trackArtwork)
                ?: user.avatarUrl,
        )
    }

    fun objectValue(source: JsonElement): JsonObject? = source as? JsonObject

    fun JsonObject.objectValue(name: String): JsonObject =
        this[name] as? JsonObject ?: JsonObject(emptyMap())

    fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())

    fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    fun JsonObject.long(name: String): Long =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: 0

    fun JsonObject.boolean(name: String): Boolean =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: false

    fun JsonObject.valueAsString(name: String): String = when (val value = this[name]) {
        null, JsonNull -> ""
        is JsonPrimitive -> value.content
        else -> value.toString()
    }

    private fun trackArtwork(value: JsonElement): String? {
        val track = value as? JsonObject ?: return null
        return track.string("artwork_url")?.takeIf(String::isNotBlank)
            ?: track.objectValue("user").string("avatar_url")?.takeIf(String::isNotBlank)
    }
}
