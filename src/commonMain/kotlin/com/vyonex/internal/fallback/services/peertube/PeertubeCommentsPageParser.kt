// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal data class PeertubeCommentsPagePayload(
    val error: String?,
    val total: Long?,
    val comments: List<PeertubeCommentPayload>?,
    val commentCount: Int?,
    val threads: List<PeertubeCommentThreadPayload>,
    val hasThreadsField: Boolean,
)

internal data class PeertubeCommentPayload(
    val id: String?,
    val threadId: String?,
    val text: String?,
    val createdAt: String?,
    val deleted: Boolean,
    val totalReplies: Long?,
    val hasAuthorReply: Boolean,
    val account: JsonObject,
)

internal data class PeertubeCommentThreadPayload(
    val comment: PeertubeCommentPayload?,
    val children: List<JsonElement>,
)

internal object PeertubeCommentsPageParser {
    fun parse(source: String): PeertubeCommentsPagePayload {
        val root = try {
            Json.parseToJsonElement(source) as? JsonObject
                ?: throw PeertubeCommentsPageParseException()
        } catch (error: SerializationException) {
            throw PeertubeCommentsPageParseException(error)
        }
        val data = root["data"] as? JsonArray
        val threads = root["children"] as? JsonArray
        return PeertubeCommentsPagePayload(
            error = root.string("error"),
            total = root.long("total"),
            comments = data?.mapNotNull { (it as? JsonObject)?.let(::comment) },
            commentCount = data?.size,
            threads = threads.orEmpty().mapNotNull(::thread),
            hasThreadsField = "children" in root,
        )
    }

    private fun thread(value: JsonElement): PeertubeCommentThreadPayload? {
        val item = value as? JsonObject ?: return null
        val children = item["children"] as? JsonArray ?: JsonArray(emptyList())
        return PeertubeCommentThreadPayload(
            comment = (item["comment"] as? JsonObject)?.let(::comment),
            children = children,
        )
    }

    private fun comment(item: JsonObject): PeertubeCommentPayload = PeertubeCommentPayload(
        id = item.numberText("id"),
        threadId = item.numberText("threadId"),
        text = item.string("text"),
        createdAt = item.string("createdAt"),
        deleted = item.boolean("isDeleted"),
        totalReplies = item.long("totalReplies"),
        hasAuthorReply = item.long("totalRepliesFromVideoAuthor")?.let { it > 0 } ?: false,
        account = item["account"] as? JsonObject ?: JsonObject(emptyMap()),
    )

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.numberText(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString || it == JsonNull }
            ?.takeIf { it.longOrNull != null || it.doubleOrNull != null }?.content

    private fun JsonObject.long(name: String): Long? =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString || it == JsonNull }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }

    private fun JsonObject.boolean(name: String): Boolean =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: false
}

internal class PeertubeCommentsPageParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid PeerTube comments response", cause)
