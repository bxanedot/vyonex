// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.soundcloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal data class SoundcloudTrackPayload(
    val policy: String,
    val id: String,
    val title: String,
    val createdAt: String?,
    val artworkUrl: String?,
    val description: String,
    val durationMillis: Long,
    val playbackCount: Long,
    val likesCount: Long,
    val userPermalinkUrl: String,
    val userName: String,
    val userAvatarUrl: String,
    val userVerified: Boolean,
    val streamable: Boolean,
    val trackAuthorization: String?,
    val sharing: String,
    val genre: String,
    val license: String,
    val tagList: String,
    val transcodings: List<SoundcloudTranscoding>,
)

internal object SoundcloudTrackPayloadParser {
    fun parseId(source: String): String? {
        val response = Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("SoundCloud resolution must be a JSON object")
        val id = response["id"]?.takeUnless { it == JsonNull } ?: return null
        return if (id is JsonPrimitive) id.content else id.toString()
    }

    fun parse(source: String): SoundcloudTrackPayload {
        val track = Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("SoundCloud track response must be a JSON object")
        val user = track.objectValue("user")
        val transcodings = track.objectValue("media").array("transcodings")
            .mapNotNull(::transcoding)
        return SoundcloudTrackPayload(
            policy = track.string("policy") ?: "",
            id = track.valueAsString("id"),
            title = track.string("title") ?: "",
            createdAt = track.string("created_at"),
            artworkUrl = track.string("artwork_url") ?: user.string("avatar_url"),
            description = track.string("description") ?: "",
            durationMillis = track.long("duration", 0),
            playbackCount = track.long("playback_count", 0),
            likesCount = track.long("likes_count", -1),
            userPermalinkUrl = user.string("permalink_url") ?: "",
            userName = user.string("username") ?: "",
            userAvatarUrl = user.string("avatar_url") ?: "",
            userVerified = user.boolean("verified"),
            streamable = track.boolean("streamable"),
            trackAuthorization = track.string("track_authorization"),
            sharing = track.string("sharing") ?: "",
            genre = track.string("genre") ?: "",
            license = track.string("license") ?: "",
            tagList = track.string("tag_list") ?: "",
            transcodings = transcodings,
        )
    }

    private fun transcoding(value: JsonElement): SoundcloudTranscoding? {
        val item = value as? JsonObject ?: return null
        return SoundcloudTranscoding(
            endpoint = item.string("url"),
            protocol = item.objectValue("format").string("protocol") ?: "",
            preset = item.string("preset") ?: " ",
        )
    }

    private fun JsonObject.valueAsString(name: String): String = when (val value = this[name]) {
        null, JsonNull -> "null"
        is JsonPrimitive -> value.content
        else -> value.toString()
    }

    private fun JsonObject.objectValue(name: String): JsonObject =
        this[name] as? JsonObject ?: JsonObject(emptyMap())

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.long(name: String, fallback: Long): Long =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: fallback

    private fun JsonObject.boolean(name: String): Boolean =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: false
}
