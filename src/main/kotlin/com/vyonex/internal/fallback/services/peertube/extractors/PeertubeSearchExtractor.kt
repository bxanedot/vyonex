// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.InfoItem
import com.vyonex.internal.fallback.MetaInfo
import com.vyonex.internal.fallback.MultiInfoItemsCollector
import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler
import com.vyonex.internal.fallback.search.SearchExtractor
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
import com.vyonex.internal.json.JsonObject
import java.io.IOException

public class PeertubeSearchExtractor(
    service: StreamingService,
    handler: SearchQueryHandler,
    private val sepia: Boolean,
) : SearchExtractor(service, handler) {
    @Throws(ParsingException::class)
    override fun getSearchSuggestion(): String = ""

    @Throws(ParsingException::class)
    override fun isCorrectedSearch(): Boolean = false

    @Throws(ParsingException::class)
    override fun getMetaInfo(): List<MetaInfo> = emptyList()

    @Throws(IOException::class, ExtractionException::class)
    override fun getInitialPage(): InfoItemsPage<InfoItem> = getPage(
        Page(getUrl() + "&${PeertubeParsingHelper.START_KEY}=0"
            + "&${PeertubeParsingHelper.COUNT_KEY}=${PeertubeParsingHelper.ITEMS_PER_PAGE}"),
    )

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<InfoItem> {
        val url = page?.url?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Page URL is required")
        val data: JsonObject = PeertubeParsingHelper.readApiObject(
            getDownloader().get(url), "search results",
        )
        PeertubeParsingHelper.validate(data)

        val items = MultiInfoItemsCollector(getServiceId())
        PeertubeParsingHelper.collectItemsFrom(items, data, getBaseUrl(), sepia)
        return InfoItemsPage(items, PeertubeParsingHelper.getNextPage(url, data.getLong("total")))
    }

    override fun onFetchPage(downloader: Downloader) = Unit
}
