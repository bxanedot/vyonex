// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.comments.CommentsInfoItemExtractor
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.localization.DateWrapper
import com.vyonex.internal.fallback.services.peertube.PeertubeCommentPayload
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory
import com.vyonex.internal.fallback.stream.Description
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jsoup.Jsoup

internal class PeertubeCommentsInfoItemExtractor(
    item: PeertubeCommentPayload,
    children: List<JsonElement>?,
    private val commentsUrl: String,
    private val baseUrl: String,
    reply: Boolean,
) : CommentsInfoItemExtractor {
    private val account = item.account
    private val displayName = (account["displayName"] as? JsonPrimitive)
        ?.takeIf { it.isString }?.content
    private val createdAt = item.createdAt
    private val text = item.text
    private val id = item.id
    private val threadId = item.threadId
    private val accountName = (account["name"] as? JsonPrimitive)
        ?.takeIf { it.isString }?.content
    private val accountHost = (account["host"] as? JsonPrimitive)
        ?.takeIf { it.isString }?.content
    private val avatars: List<Image> = PeertubeParsingHelper
        .getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, account)
    private val childCount = children?.size
    private val repliesBody = if (reply && !children.isNullOrEmpty()) encodeReplies(children) else null
    private val totalReplies = item.totalReplies
    private val hasAuthorReply = item.hasAuthorReply

    @Throws(ParsingException::class)
    override fun getUrl(): String = "$commentsUrl/${commentId()}"

    override fun getThumbnails(): List<Image> = avatars

    @Throws(ParsingException::class)
    override fun getName(): String = required(displayName, "account.displayName")

    @Throws(ParsingException::class)
    override fun getTextualUploadDate(): String = required(createdAt, "createdAt")

    @Throws(ParsingException::class)
    override fun getUploadDate(): DateWrapper? = DateWrapper.fromInstant(required(createdAt, "createdAt"))

    @Throws(ParsingException::class)
    override fun getCommentText(): Description = Description(
        Jsoup.parse(required(text, "text")).text(), Description.PLAIN_TEXT,
    )

    @Throws(ParsingException::class)
    override fun getCommentId(): String = commentId()

    override fun getUploaderAvatars(): List<Image> = avatars

    @Throws(ParsingException::class)
    override fun getUploaderName(): String = accountHandle()

    @Throws(ParsingException::class)
    override fun getUploaderUrl(): String = PeertubeChannelLinkHandlerFactory.getInstance()
        .fromId("accounts/${accountHandle()}", baseUrl).url

    @Throws(ParsingException::class)
    override fun getReplies(): Page? {
        if (getReplyCount() == 0) return null
        val id = requiredNumericText(threadId, "threadId")
        val url = "$commentsUrl/$id"
        return repliesBody?.let { Page(url, id, it.clone()) } ?: Page(url, id)
    }

    @Throws(ParsingException::class)
    override fun getReplyCount(): Int = childCount ?: required(totalReplies, "totalReplies").toInt()

    override fun hasCreatorReply(): Boolean = hasAuthorReply

    private fun commentId(): String = requiredNumericText(id, "id")

    private fun accountHandle(): String =
        "${required(accountName, "account.name")}@${required(accountHost, "account.host")}"

    private fun encodeReplies(children: List<JsonElement>): ByteArray = Json.encodeToString(
        JsonObject.serializer(), JsonObject(mapOf(CHILDREN to JsonArray(children))),
    ).encodeToByteArray()

    private fun required(value: String?, field: String): String = value
        ?: throw ParsingException("Missing string field: $field")

    private fun required(value: Long?, field: String): Long = value
        ?: throw ParsingException("Missing numeric field: $field")

    private fun requiredNumericText(value: String?, field: String): String = value
        ?: throw ParsingException("Missing numeric field: $field")

    private companion object {
        const val CHILDREN = "children"
    }
}
