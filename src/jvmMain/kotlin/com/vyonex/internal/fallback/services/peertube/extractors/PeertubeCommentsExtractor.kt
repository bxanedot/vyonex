// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.comments.CommentsExtractor
import com.vyonex.internal.fallback.comments.CommentsInfoItem
import com.vyonex.internal.fallback.comments.CommentsInfoItemsCollector
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
import com.vyonex.internal.json.JsonArray
import com.vyonex.internal.json.JsonObject
import com.vyonex.internal.json.JsonParser
import com.vyonex.internal.json.JsonParserException
import java.io.IOException
import java.nio.charset.StandardCharsets

public class PeertubeCommentsExtractor(
    service: StreamingService,
    handler: ListLinkHandler,
) : CommentsExtractor(service, handler) {
    @Throws(IOException::class, ExtractionException::class)
    override fun getInitialPage(): InfoItemsPage<CommentsInfoItem> {
        val query = "?${PeertubeParsingHelper.START_KEY}=0&" +
            "${PeertubeParsingHelper.COUNT_KEY}=${PeertubeParsingHelper.ITEMS_PER_PAGE}"
        val url = if (isReply()) getUrl() else getUrl() + query
        return getPage(Page(url))
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<CommentsInfoItem> {
        val url = page?.url?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Page URL is required")
        val body = page.body
        val data = if (body != null) parseEmbedded(body)
        else PeertubeParsingHelper.readApiObject(getDownloader().get(url), "comment data")
        PeertubeParsingHelper.validate(data)

        val items = CommentsInfoItemsCollector(getServiceId())
        if (body != null || page.id != null || data.has(CHILDREN) || isReply()) {
            collectReplies(items, data.getArray(CHILDREN))
            return InfoItemsPage(items, null)
        }

        val comments = data.getArray("data", null)
            ?: throw ParsingException("Comment response has no data array")
        collectComments(items, comments)
        val total = data.getLong("total", comments.size.toLong())
        return InfoItemsPage(items, PeertubeParsingHelper.getNextPage(url, total))
    }

    private fun parseEmbedded(body: ByteArray): JsonObject = try {
        JsonParser.`object`().from(String(body, StandardCharsets.UTF_8))
    } catch (error: JsonParserException) {
        throw ParsingException("Invalid embedded comment replies", error)
    }

    private fun collectComments(items: CommentsInfoItemsCollector, comments: JsonArray) {
        for (value in comments) {
            val comment = value as? JsonObject ?: continue
            if (!comment.getBoolean("isDeleted")) {
                items.commit(PeertubeCommentsInfoItemExtractor(
                    comment, null, getUrl(), getBaseUrl(), false,
                ))
            }
        }
    }

    private fun collectReplies(items: CommentsInfoItemsCollector, replies: JsonArray) {
        for (value in replies) {
            val node = value as? JsonObject ?: continue
            val comment = node.getObject("comment", null) ?: continue
            if (!comment.getBoolean("isDeleted")) {
                items.commit(PeertubeCommentsInfoItemExtractor(
                    comment, node.getArray(CHILDREN), getUrl(), getBaseUrl(), true,
                ))
            }
        }
    }

    private fun isReply(): Boolean = getOriginalUrl().contains("/comment-threads/")

    override fun onFetchPage(downloader: Downloader) = Unit

    private companion object {
        const val CHILDREN = "children"
    }
}
