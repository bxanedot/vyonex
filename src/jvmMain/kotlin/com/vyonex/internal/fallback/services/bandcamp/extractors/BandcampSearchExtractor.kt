// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.InfoItem
import com.vyonex.internal.fallback.MetaInfo
import com.vyonex.internal.fallback.MultiInfoItemsCollector
import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler
import com.vyonex.internal.fallback.search.SearchExtractor
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampSearchItem.Album
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampSearchItem.Artist
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampSearchItem.Track
import com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem.BandcampSearchStreamInfoItemExtractor
import org.jsoup.nodes.Document
import java.io.IOException

public class BandcampSearchExtractor(
    service: StreamingService,
    queryHandler: SearchQueryHandler,
) : SearchExtractor(service, queryHandler) {
    override fun getSearchSuggestion(): String = ""

    override fun isCorrectedSearch(): Boolean = false

    override fun getMetaInfo(): List<MetaInfo> = emptyList()

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page): InfoItemsPage<InfoItem> {
        val html = downloader.get(
            page.url,
            mapOf("Cookie" to listOf("identity")),
        ).responseBody()
        val parsed = BandcampSearchPageParser.parse(html, page.url)
        val items = MultiInfoItemsCollector(serviceId)
        for (result in parsed.items) {
            when (result) {
                is Artist -> items.commit(BandcampChannelInfoItemExtractor(result))
                is Album -> items.commit(BandcampPlaylistInfoItemExtractor(result))
                is Track -> items.commit(BandcampSearchStreamInfoItemExtractor(result, null))
            }
        }
        return InfoItemsPage(items, parsed.nextPageUrl?.let(::Page))
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getInitialPage(): InfoItemsPage<InfoItem> = getPage(Page(getUrl()))

    override fun onFetchPage(downloader: Downloader) = Unit

    public companion object {
        @JvmStatic
        public fun findNextPage(document: Document, currentUrl: String): Page? {
            return BandcampSearchPageParser.findNextPageUrl(document, currentUrl)?.let(::Page)
        }
    }
}
