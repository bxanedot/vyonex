// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.comments.CommentsExtractor
import com.vyonex.internal.fallback.comments.CommentsInfoItem
import com.vyonex.internal.fallback.comments.CommentsInfoItemsCollector
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.exceptions.ReCaptchaException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.services.bandcamp.BandcampReviewPageParseException
import com.vyonex.internal.fallback.services.bandcamp.BandcampReviewPageParser
import com.vyonex.internal.fallback.services.bandcamp.BandcampReviewPayload
import com.vyonex.internal.json.JsonWriter
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.nio.charset.StandardCharsets

public class BandcampCommentsExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : CommentsExtractor(service, linkHandler) {
    private var document: Document? = null

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        document = Jsoup.parse(downloader.get(linkHandler.url).responseBody())
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getInitialPage(): InfoItemsPage<CommentsInfoItem> {
        val data = try {
            BandcampReviewPageParser.parseEmbedded(embeddedData())
        } catch (error: BandcampReviewPageParseException) {
            throw ParsingException("Bandcamp review data is invalid", error)
        }
        val items = collect(data.reviews)
        if (!data.moreAvailable) return InfoItemsPage(items, null)
        return InfoItemsPage(items, Page(listOf(trackId(), nextToken(data.nextToken))))
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page): InfoItemsPage<CommentsInfoItem> {
        val ids = page.ids
        if (ids.size != 2) throw ParsingException("Invalid Bandcamp review cursor")
        val body = JsonWriter.string().`object`()
            .value("tralbum_type", "t")
            .value("tralbum_id", ids[0])
            .value("token", ids[1])
            .value("count", 7)
            .array("exclude_fan_ids").end()
            .end().done()
            .toByteArray(StandardCharsets.UTF_8)
        val response = try {
            BandcampReviewPageParser.parseResponse(
                downloader.postWithContentTypeJson(REVIEWS_API_URL, emptyMap(), body).responseBody(),
            )
        } catch (error: ReCaptchaException) {
            throw ParsingException("Bandcamp reviews response is invalid", error)
        } catch (error: BandcampReviewPageParseException) {
            throw ParsingException("Bandcamp reviews response is invalid", error)
        }
        val items = collect(response.reviews)
        if (!response.moreAvailable) return InfoItemsPage(items, null)
        return InfoItemsPage(items, Page(listOf(ids[0], nextToken(response.nextToken))))
    }

    @Throws(ExtractionException::class)
    override fun isCommentsDisabled(): Boolean = BandcampExtractorHelper.isRadioUrl(url)

    private fun collect(reviews: List<BandcampReviewPayload>): CommentsInfoItemsCollector {
        val items = CommentsInfoItemsCollector(serviceId)
        for (review in reviews) {
            items.commit(BandcampCommentsInfoItemExtractor(review, url))
        }
        return items
    }

    @Throws(ParsingException::class)
    private fun embeddedData(): String {
        val element = pageDocument().selectFirst("#collectors-data[$REVIEW_DATA_ATTRIBUTE]")
            ?: throw ParsingException("Bandcamp page has no review data")
        return element.attr(REVIEW_DATA_ATTRIBUTE)
    }

    @Throws(ParsingException::class)
    private fun trackId(): String {
        val properties = pageDocument().selectFirst("meta[name=bc-page-properties][content]")
            ?: throw ParsingException("Bandcamp page has no item metadata")
        val id = try {
            BandcampReviewPageParser.parseTrackId(properties.attr("content"))
        } catch (error: BandcampReviewPageParseException) {
            throw ParsingException("Bandcamp item metadata is invalid", error)
        }
        if (id <= 0) throw ParsingException("Bandcamp page has an invalid item ID")
        return id.toString()
    }

    @Throws(ParsingException::class)
    private fun nextToken(token: String?): String = token?.takeIf(String::isNotBlank)
        ?: throw ParsingException("Bandcamp reviews did not include a next-page token")

    @Throws(ParsingException::class)
    private fun pageDocument(): Document = document
        ?: throw ParsingException("Bandcamp comments page has not been fetched")

    public companion object {
        private const val REVIEW_DATA_ATTRIBUTE = "data-blob"
        public const val REVIEWS_API_URL = "${BandcampExtractorHelper.BASE_API_URL}/tralbumcollectors/2/reviews"
    }
}
