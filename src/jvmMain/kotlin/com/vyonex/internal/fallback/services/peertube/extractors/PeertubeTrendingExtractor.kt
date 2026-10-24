// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.kiosk.KioskExtractor
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
import com.vyonex.internal.fallback.services.peertube.PeertubeVideoListPageParseException
import com.vyonex.internal.fallback.services.peertube.PeertubeVideoListPageParser
import com.vyonex.internal.fallback.stream.StreamInfoItem
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector
import com.vyonex.internal.json.JsonParser
import java.io.IOException

public class PeertubeTrendingExtractor(
    service: StreamingService,
    handler: ListLinkHandler,
    kioskId: String,
) : KioskExtractor<StreamInfoItem>(service, handler, kioskId) {
    override fun getName(): String = getId()

    @Throws(IOException::class, ExtractionException::class)
    override fun getInitialPage(): InfoItemsPage<StreamInfoItem> = getPage(
        Page(getUrl() + "&${PeertubeParsingHelper.START_KEY}=0"
            + "&${PeertubeParsingHelper.COUNT_KEY}=${PeertubeParsingHelper.ITEMS_PER_PAGE}"),
    )

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> {
        val url = page?.url?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Page URL is required")
        val body = PeertubeParsingHelper.readApiBody(getDownloader().get(url), "kiosk results")
        val payload = try {
            PeertubeVideoListPageParser.parse(body)
        } catch (error: PeertubeVideoListPageParseException) {
            throw ParsingException("Invalid PeerTube kiosk results response", error)
        }
        payload.error?.let { throw ContentNotAvailableException(it) }
        val data = payload.data ?: throw ParsingException("Response data is not an array")

        val items = StreamInfoItemsCollector(getServiceId())
        val response = JsonParser.`object`().from("""{"data":$data}""")
        PeertubeParsingHelper.collectItemsFrom(items, response, getBaseUrl())
        return InfoItemsPage(items, PeertubeParsingHelper.getNextPage(url, payload.total))
    }

    override fun onFetchPage(downloader: Downloader) = Unit
}
