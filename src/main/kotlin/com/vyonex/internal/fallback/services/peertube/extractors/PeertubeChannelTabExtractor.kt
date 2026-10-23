// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.InfoItem
import com.vyonex.internal.fallback.MultiInfoItemsCollector
import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory
import java.io.IOException

public class PeertubeChannelTabExtractor @Throws(ParsingException::class) constructor(
    service: StreamingService,
    handler: ListLinkHandler,
) : ChannelTabExtractor(service, handler) {
    private val baseUrl = getBaseUrl()

    override fun onFetchPage(downloader: Downloader) = Unit

    @Throws(IOException::class, ExtractionException::class)
    override fun getInitialPage(): InfoItemsPage<InfoItem> {
        val root = "$baseUrl${PeertubeChannelLinkHandlerFactory.API_ENDPOINT}${getId()}"
        val suffix = PeertubeChannelTabLinkHandlerFactory.getUrlSuffix(getName())
        val query = "${PeertubeParsingHelper.START_KEY}=0&" +
            "${PeertubeParsingHelper.COUNT_KEY}=${PeertubeParsingHelper.ITEMS_PER_PAGE}"
        val url = "$root$suffix?$query"
        return getPage(Page(url))
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<InfoItem> {
        val url = page?.url?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Page URL is required")
        val data = PeertubeParsingHelper.readApiObject(getDownloader().get(url), "channel tab data")
        PeertubeParsingHelper.validate(data)

        val items = MultiInfoItemsCollector(getServiceId())
        PeertubeParsingHelper.collectItemsFrom(items, data, baseUrl)
        return InfoItemsPage(items, PeertubeParsingHelper.getNextPage(url, data.getLong("total")))
    }
}
