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
import com.vyonex.internal.fallback.services.bandcamp.BandcampFeaturedPageParseException
import com.vyonex.internal.fallback.services.bandcamp.BandcampFeaturedPageParser
import com.vyonex.internal.fallback.services.bandcamp.BandcampFeaturedPagePayload
import com.vyonex.internal.fallback.services.bandcamp.BandcampFeaturedStoriesMissingException
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
        val response = downloader.postWithContentTypeJson(FEATURED_API_URL, emptyMap(), body).responseBody()
        responsePage = try {
            extract(BandcampFeaturedPageParser.parseInitial(response))
        } catch (error: BandcampFeaturedStoriesMissingException) {
            throw ParsingException("Bandcamp featured response has no stories", error)
        } catch (error: BandcampFeaturedPageParseException) {
            throw ParsingException("Bandcamp featured response is invalid", error)
        }
    }

    override fun getName(): String = KIOSK_FEATURED

    override fun getInitialPage(): InfoItemsPage<PlaylistInfoItem> = checkNotNull(responsePage) {
        "Bandcamp featured page has not been fetched"
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page): InfoItemsPage<PlaylistInfoItem> {
        val response = getDownloader().get(page.url).responseBody()
        val pageResult = try {
            extract(BandcampFeaturedPageParser.parseOlder(response))
        } catch (error: BandcampFeaturedStoriesMissingException) {
            throw ParsingException("Bandcamp featured response has no stories", error)
        } catch (error: BandcampFeaturedPageParseException) {
            throw ParsingException("Bandcamp featured page is invalid", error)
        }
        responsePage = pageResult
        return pageResult
    }

    private fun extract(data: BandcampFeaturedPagePayload): InfoItemsPage<PlaylistInfoItem> {
        val items = PlaylistInfoItemsCollector(serviceId)
        for (story in data.items) {
            items.commit(BandcampPlaylistInfoItemFeaturedExtractor(story))
        }
        return InfoItemsPage(items, data.nextStoryGroup?.let(::nextPage))
    }

    private fun nextPage(storyGroup: String): Page = Page("$MORE_FEATURED_API_URL?story_groups=$storyGroup")

    public companion object {
        public const val KIOSK_FEATURED = "Featured"
        public const val FEATURED_API_URL = "${BandcampExtractorHelper.BASE_API_URL}/mobile/24/bootstrap_data"
        public const val MORE_FEATURED_API_URL = "${BandcampExtractorHelper.BASE_API_URL}/mobile/24/feed_older_logged_out"
    }
}
