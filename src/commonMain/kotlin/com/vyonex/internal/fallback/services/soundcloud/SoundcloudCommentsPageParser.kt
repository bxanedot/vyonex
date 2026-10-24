// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal data class SoundcloudCommentInfoItem(
    val id: String?,
    val body: String,
    val user: SoundcloudProfileInfoItem,
    val name: String,
    val timestampMillis: Long,
    val createdAt: String,
)

internal data class SoundcloudCommentsPagePayload(
    val comments: List<SoundcloudCommentInfoItem>,
    val nextPageUrl: String?,
)

internal object SoundcloudCommentsPageParser {
    fun parse(source: String): SoundcloudCommentsPagePayload {
        val response = parseObject(source)
        val collection = SoundcloudInfoItemPayloadParser.run { response.array("collection") }
        return SoundcloudCommentsPagePayload(
            comments = collection.mapNotNull { value ->
                val comment = SoundcloudInfoItemPayloadParser.objectValue(value) ?: return@mapNotNull null
                val user = SoundcloudInfoItemPayloadParser.run { comment.objectValue("user") }
                SoundcloudCommentInfoItem(
                    id = commentId(comment["id"]),
                    body = SoundcloudInfoItemPayloadParser.run { comment.string("body") } ?: "",
                    user = SoundcloudInfoItemPayloadParser.profile(user),
                    name = SoundcloudInfoItemPayloadParser.run { user.string("permalink") } ?: "",
                    timestampMillis = SoundcloudInfoItemPayloadParser.run { comment.long("timestamp") },
                    createdAt = SoundcloudInfoItemPayloadParser.run { comment.string("created_at") } ?: "",
                )
            },
            nextPageUrl = SoundcloudInfoItemPayloadParser.run { response.string("next_href") },
        )
    }

    private fun commentId(value: kotlinx.serialization.json.JsonElement?): String? = when (value) {
        null, JsonNull -> null
        is JsonPrimitive -> value.content
        else -> value.toString()
    }

    private fun parseObject(source: String): JsonObject = try {
        Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("SoundCloud comments response must be a JSON object")
    } catch (error: SerializationException) {
        throw IllegalArgumentException("Invalid SoundCloud comments response", error)
    }
}
