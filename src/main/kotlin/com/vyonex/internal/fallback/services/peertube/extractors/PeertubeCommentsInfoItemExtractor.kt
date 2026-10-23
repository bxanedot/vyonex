// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.comments.CommentsInfoItemExtractor
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.localization.DateWrapper
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory
import com.vyonex.internal.fallback.stream.Description
import com.vyonex.internal.json.JsonArray
import com.vyonex.internal.json.JsonObject
import com.vyonex.internal.json.JsonWriter
import org.jsoup.Jsoup
import java.nio.charset.StandardCharsets

public class PeertubeCommentsInfoItemExtractor(
    item: JsonObject,
    children: JsonArray?,
    private val commentsUrl: String,
    private val baseUrl: String,
    reply: Boolean,
) : CommentsInfoItemExtractor {
    private val account = item.getObject("account")
    private val displayName = account.getString("displayName")
    private val createdAt = item.getString("createdAt")
    private val text = item.getString("text")
    private val id = item.getNumber("id")
    private val threadId = item.getNumber("threadId")
    private val accountName = account.getString("name")
    private val accountHost = account.getString("host")
    private val avatars: List<Image> = PeertubeParsingHelper
        .getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, account)
    private val childCount = children?.size
    private val repliesBody = if (reply && !children.isNullOrEmpty()) encodeReplies(children) else null
    private val totalReplies = item.getNumber("totalReplies")
    private val hasAuthorReply = item.getLong("totalRepliesFromVideoAuthor") > 0

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
        val id = required(threadId, "threadId").toString()
        val url = "$commentsUrl/$id"
        return repliesBody?.let { Page(url, id, it.clone()) } ?: Page(url, id)
    }

    @Throws(ParsingException::class)
    override fun getReplyCount(): Int = childCount ?: required(totalReplies, "totalReplies").toInt()

    override fun hasCreatorReply(): Boolean = hasAuthorReply

    private fun commentId(): String = required(id, "id").toString()

    private fun accountHandle(): String =
        "${required(accountName, "account.name")}@${required(accountHost, "account.host")}"

    private fun encodeReplies(children: JsonArray): ByteArray {
        val body = JsonObject()
        body[CHILDREN] = children
        return JsonWriter.string(body).toByteArray(StandardCharsets.UTF_8)
    }

    private fun required(value: String?, field: String): String = value
        ?: throw ParsingException("Missing string field: $field")

    private fun required(value: Number?, field: String): Number = value
        ?: throw ParsingException("Missing numeric field: $field")

    private companion object {
        const val CHILDREN = "children"
    }
}
