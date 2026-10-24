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
import com.vyonex.internal.json.JsonArray
import com.vyonex.internal.json.JsonObject
import com.vyonex.internal.json.JsonParser
import com.vyonex.internal.json.JsonParserException
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
        val data = embeddedData("#collectors-data", "data-blob")
        val reviews = data.getArray("reviews")
            ?: throw ParsingException("Bandcamp review data has no reviews")
        val items = collect(reviews)
        if (!data.getBoolean("more_reviews_available")) return InfoItemsPage(items, null)
        return InfoItemsPage(items, Page(listOf(trackId(), nextToken(reviews))))
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
            JsonParser.`object`().from(
                downloader.postWithContentTypeJson(REVIEWS_API_URL, emptyMap(), body)
                    .responseBody(),
            )
        } catch (error: ReCaptchaException) {
            throw ParsingException("Bandcamp reviews response is invalid", error)
        } catch (error: JsonParserException) {
            throw ParsingException("Bandcamp reviews response is invalid", error)
        }
        val reviews = response.getArray("results")
            ?: throw ParsingException("Bandcamp reviews response has no results")
        val items = collect(reviews)
        if (!response.getBoolean("more_available")) return InfoItemsPage(items, null)
        return InfoItemsPage(items, Page(listOf(ids[0], nextToken(reviews))))
    }

    @Throws(ExtractionException::class)
    override fun isCommentsDisabled(): Boolean = BandcampExtractorHelper.isRadioUrl(url)

    private fun collect(reviews: JsonArray): CommentsInfoItemsCollector {
        val items = CommentsInfoItemsCollector(serviceId)
        for (value in reviews) {
            val review = value as? JsonObject ?: continue
            items.commit(BandcampCommentsInfoItemExtractor(review, url))
        }
        return items
    }

    @Throws(ParsingException::class)
    private fun embeddedData(selector: String, attribute: String): JsonObject {
        val element = pageDocument().selectFirst("$selector[$attribute]")
            ?: throw ParsingException("Bandcamp page has no review data")
        return try {
            JsonParser.`object`().from(element.attr(attribute))
        } catch (error: JsonParserException) {
            throw ParsingException("Bandcamp review data is invalid", error)
        }
    }

    @Throws(ParsingException::class)
    private fun trackId(): String {
        val properties = pageDocument().selectFirst("meta[name=bc-page-properties][content]")
            ?: throw ParsingException("Bandcamp page has no item metadata")
        val id = try {
            JsonParser.`object`().from(properties.attr("content")).getLong("item_id")
        } catch (error: JsonParserException) {
            throw ParsingException("Bandcamp item metadata is invalid", error)
        }
        if (id <= 0) throw ParsingException("Bandcamp page has an invalid item ID")
        return id.toString()
    }

    @Throws(ParsingException::class)
    private fun nextToken(reviews: JsonArray): String {
        var token: String? = null
        for (value in reviews) {
            val review = value as? JsonObject ?: continue
            token = review.getString("token", token)
        }
        return token?.takeIf(String::isNotBlank)
            ?: throw ParsingException("Bandcamp reviews did not include a next-page token")
    }

    @Throws(ParsingException::class)
    private fun pageDocument(): Document = document
        ?: throw ParsingException("Bandcamp comments page has not been fetched")

    public companion object {
        public const val REVIEWS_API_URL = "${BandcampExtractorHelper.BASE_API_URL}/tralbumcollectors/2/reviews"
    }
}
