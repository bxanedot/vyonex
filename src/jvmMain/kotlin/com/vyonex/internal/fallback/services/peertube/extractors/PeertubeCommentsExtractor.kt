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
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.services.peertube.PeertubeCommentsPageParseException
import com.vyonex.internal.fallback.services.peertube.PeertubeCommentsPageParser
import com.vyonex.internal.fallback.services.peertube.PeertubeCommentPayload
import com.vyonex.internal.fallback.services.peertube.PeertubeCommentThreadPayload
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
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
        val source = body?.let { String(it, StandardCharsets.UTF_8) }
            ?: PeertubeParsingHelper.readApiBody(getDownloader().get(url), "comment data")
        val data = try {
            PeertubeCommentsPageParser.parse(source)
        } catch (error: PeertubeCommentsPageParseException) {
            val message = if (body != null) "Invalid embedded comment replies"
            else "Invalid PeerTube comment data response"
            throw ParsingException(message, error)
        }
        data.error?.takeIf(String::isNotBlank)?.let { throw ContentNotAvailableException(it) }

        val items = CommentsInfoItemsCollector(getServiceId())
        if (body != null || page.id != null || data.hasThreadsField || isReply()) {
            collectReplies(items, data.threads)
            return InfoItemsPage(items, null)
        }

        val comments = data.comments
            ?: throw ParsingException("Comment response has no data array")
        collectComments(items, comments)
        val total = data.total ?: data.commentCount?.toLong() ?: 0L
        return InfoItemsPage(items, PeertubeParsingHelper.getNextPage(url, total))
    }

    private fun collectComments(items: CommentsInfoItemsCollector, comments: List<PeertubeCommentPayload>) {
        for (comment in comments) {
            if (!comment.deleted) {
                items.commit(PeertubeCommentsInfoItemExtractor(
                    comment, null, getUrl(), getBaseUrl(), false,
                ))
            }
        }
    }

    private fun collectReplies(items: CommentsInfoItemsCollector, replies: List<PeertubeCommentThreadPayload>) {
        for (node in replies) {
            val comment = node.comment ?: continue
            if (!comment.deleted) {
                items.commit(PeertubeCommentsInfoItemExtractor(
                    comment, node.children, getUrl(), getBaseUrl(), true,
                ))
            }
        }
    }

    private fun isReply(): Boolean = getOriginalUrl().contains("/comment-threads/")

    override fun onFetchPage(downloader: Downloader) = Unit

}
