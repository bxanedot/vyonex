// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.kiosk.KioskExtractor
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.playlist.PlaylistInfoItem
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemsCollector
import com.vyonex.internal.json.JsonArray
import com.vyonex.internal.json.JsonObject
import com.vyonex.internal.json.JsonParser
import com.vyonex.internal.json.JsonParserException
import com.vyonex.internal.json.JsonWriter
import java.io.IOException
import java.nio.charset.StandardCharsets

public class BandcampFeaturedExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
) : KioskExtractor<PlaylistInfoItem>(service, linkHandler, kioskId) {
    private var responsePage: InfoItemsPage<PlaylistInfoItem>? = null

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val body = JsonWriter.string().`object`()
            .value("platform", "")
            .value("version", 0)
            .end().done()
            .toByteArray(StandardCharsets.UTF_8)
        val response = try {
            JsonParser.`object`().from(
                downloader.postWithContentTypeJson(FEATURED_API_URL, emptyMap(), body)
                    .responseBody(),
            )
        } catch (error: JsonParserException) {
            throw ParsingException("Bandcamp featured response is invalid", error)
        }
        val stories = response.getObject("feed_content")?.getObject("stories")
            ?.getArray("featured")
        responsePage = extract(stories)
    }

    override fun getName(): String = KIOSK_FEATURED

    override fun getInitialPage(): InfoItemsPage<PlaylistInfoItem> = checkNotNull(responsePage) {
        "Bandcamp featured page has not been fetched"
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page): InfoItemsPage<PlaylistInfoItem> {
        val response = try {
            JsonParser.`object`().from(getDownloader().get(page.url).responseBody())
        } catch (error: JsonParserException) {
            throw ParsingException("Bandcamp featured page is invalid", error)
        }
        val pageResult = extract(response.getObject("stories")?.getArray("featured"))
        responsePage = pageResult
        return pageResult
    }

    @Throws(ParsingException::class)
    private fun extract(stories: JsonArray?): InfoItemsPage<PlaylistInfoItem> {
        val data = stories ?: throw ParsingException("Bandcamp featured response has no stories")
        val items = PlaylistInfoItemsCollector(serviceId)
        var last: JsonObject? = null
        for (value in data) {
            val story = value as? JsonObject ?: continue
            last = story
            if (story.getString("album_title") != null) {
                items.commit(BandcampPlaylistInfoItemFeaturedExtractor(story))
            }
        }
        return InfoItemsPage(items, last?.let(::nextPage))
    }

    private fun nextPage(story: JsonObject): Page? {
        val type = story.getString("story_type") ?: return null
        if (!story.isNumber("story_date") || !story.isNumber("ntid")) return null
        val cursor = "featured:${story.getLong("story_date")}:$type:${story.getLong("ntid")}"
        return Page("$MORE_FEATURED_API_URL?story_groups=$cursor")
    }

    public companion object {
        public const val KIOSK_FEATURED = "Featured"
        public const val FEATURED_API_URL = "${BandcampExtractorHelper.BASE_API_URL}/mobile/24/bootstrap_data"
        public const val MORE_FEATURED_API_URL = "${BandcampExtractorHelper.BASE_API_URL}/mobile/24/feed_older_logged_out"
    }
}
