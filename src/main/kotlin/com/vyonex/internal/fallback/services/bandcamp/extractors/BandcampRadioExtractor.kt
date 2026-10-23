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
import com.vyonex.internal.fallback.stream.StreamInfoItem
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector
import com.vyonex.internal.json.JsonArray
import com.vyonex.internal.json.JsonObject
import com.vyonex.internal.json.JsonParser
import com.vyonex.internal.json.JsonParserException
import java.io.IOException

public class BandcampRadioExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
) : KioskExtractor<StreamInfoItem>(service, linkHandler, kioskId) {
    private var shows: List<BandcampRadioInfoItemExtractor> = emptyList()

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val response = downloader.get(RADIO_API_URL)
            ?: throw IOException("Bandcamp radio returned no response")
        if (response.responseCode() >= 400) {
            throw IOException("Bandcamp radio request failed with HTTP ${response.responseCode()}")
        }
        val payload = try {
            JsonParser.`object`().from(response.responseBody())
        } catch (error: JsonParserException) {
            throw ParsingException("Bandcamp radio response is invalid", error)
        }
        val results = payload.get("results") as? JsonArray
            ?: throw ParsingException("Bandcamp radio has no results")
        shows = results.filterIsInstance<JsonObject>()
            .map(::BandcampRadioInfoItemExtractor)
    }

    override fun getName(): String = KIOSK_RADIO

    override fun getInitialPage(): InfoItemsPage<StreamInfoItem> {
        val items = StreamInfoItemsCollector(serviceId)
        shows.forEach(items::commit)
        return InfoItemsPage(items, null)
    }

    override fun getPage(page: Page): InfoItemsPage<StreamInfoItem>? = null

    public companion object {
        public const val KIOSK_RADIO = "Radio"
        public const val RADIO_API_URL = "${BandcampExtractorHelper.BASE_API_URL}/bcweekly/3/list"
    }
}
