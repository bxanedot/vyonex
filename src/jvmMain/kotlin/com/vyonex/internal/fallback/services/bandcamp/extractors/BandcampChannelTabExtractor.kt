// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.InfoItem
import com.vyonex.internal.fallback.MultiInfoItemsCollector
import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.services.bandcamp.BandcampDiscographyItem
import com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem.BandcampDiscographStreamInfoItemExtractor
import com.vyonex.internal.json.JsonArray
import com.vyonex.internal.json.JsonObject
import java.io.IOException

public class BandcampChannelTabExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : ChannelTabExtractor(service, linkHandler) {
    private val itemType = when (
        linkHandler.contentFilters.firstOrNull()
            ?: throw IllegalArgumentException("Channel tab is missing its type")
    ) {
        ChannelTabs.TRACKS -> "track"
        ChannelTabs.ALBUMS -> "album"
        else -> throw IllegalArgumentException("Unsupported Bandcamp channel tab")
    }

    private var items: List<BandcampDiscographyItem>? = null

    @Throws(ParsingException::class)
    override fun onFetchPage(downloader: Downloader) {
        if (items == null) {
            val artist = BandcampExtractorHelper.getArtistDetailsPayload(downloader, id)
            items = selectItems(artist.discography, itemType)
        }
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getInitialPage(): InfoItemsPage<InfoItem> {
        val discography = items ?: throw ParsingException("Bandcamp tab has not been fetched")
        val collector = MultiInfoItemsCollector(serviceId)
        for (item in discography) {
            if (itemType == "track") {
                collector.commit(BandcampDiscographStreamInfoItemExtractor(item, url))
            } else {
                collector.commit(BandcampAlbumInfoItemExtractor(item, url))
            }
        }
        return InfoItemsPage(collector, null)
    }

    override fun getPage(page: Page): InfoItemsPage<InfoItem>? = null

    public companion object {
        @JvmStatic
        public fun fromDiscography(
            service: StreamingService,
            linkHandler: ListLinkHandler,
            discography: JsonArray,
        ): BandcampChannelTabExtractor = fromDiscographyPayload(
            service,
            linkHandler,
            discography.filterIsInstance<JsonObject>().map(::discographyItem),
        )

        internal fun fromDiscographyPayload(
            service: StreamingService,
            linkHandler: ListLinkHandler,
            discography: List<BandcampDiscographyItem>,
        ): BandcampChannelTabExtractor = BandcampChannelTabExtractor(service, linkHandler).apply {
            items = selectItems(discography, itemType)
        }

        private fun selectItems(
            discography: List<BandcampDiscographyItem>,
            type: String,
        ): List<BandcampDiscographyItem> =
            discography.filter { it.type == type }

        private fun discographyItem(item: JsonObject): BandcampDiscographyItem = BandcampDiscographyItem(
            type = item.getString("item_type"),
            title = item.getString("title"),
            artist = item.getString("band_name", ""),
            artistId = item.getLong("band_id"),
            itemId = item.getLong("item_id"),
            artId = item.getLong("art_id"),
        )
    }
}
