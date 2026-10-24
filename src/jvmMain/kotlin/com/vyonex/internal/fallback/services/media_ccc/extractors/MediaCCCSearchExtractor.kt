// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.InfoItem
import com.vyonex.internal.fallback.ListExtractor
import com.vyonex.internal.fallback.MetaInfo
import com.vyonex.internal.fallback.MultiInfoItemsCollector
import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.channel.ChannelInfoItem
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler
import com.vyonex.internal.fallback.search.SearchExtractor
import com.vyonex.internal.fallback.services.media_ccc.extractors.infoItems.MediaCCCStreamInfoItemExtractor
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCEventSearchPayload
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCListingPayloadDecoder
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCListingParseException
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferencesListLinkHandlerFactory
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCSearchQueryHandlerFactory
import java.util.Locale

public class MediaCCCSearchExtractor(
    service: StreamingService,
    query: SearchQueryHandler,
) : SearchExtractor(service, query) {
    private var eventResults: List<MediaCCCStreamInfoItemExtractor>? = null
    private var conferenceResults: MediaCCCConferenceKiosk? = null

    override fun getSearchSuggestion(): String = ""

    override fun isCorrectedSearch(): Boolean = false

    override fun getMetaInfo(): List<MetaInfo> = emptyList()

    override fun getInitialPage(): ListExtractor.InfoItemsPage<InfoItem> {
        val items = MultiInfoItemsCollector(serviceId)
        val filters = linkHandler.contentFilters
        if (includes(filters, MediaCCCSearchQueryHandlerFactory.CONFERENCES)) {
            val conferences = conferenceResults
                ?: throw IllegalStateException("Conference search was not fetched")
            addMatchingConferences(items, conferences.initialPage.items)
        }
        if (includes(filters, MediaCCCSearchQueryHandlerFactory.EVENTS)) {
            eventResults?.forEach(items::commit)
                ?: throw IllegalStateException("Event search was not fetched")
        }
        return ListExtractor.InfoItemsPage(items, null)
    }

    override fun getPage(page: Page): ListExtractor.InfoItemsPage<InfoItem> =
        ListExtractor.InfoItemsPage.emptyPage()

    override fun onFetchPage(downloader: Downloader) {
        eventResults = null
        conferenceResults = null
        val filters = linkHandler.contentFilters
        if (includes(filters, MediaCCCSearchQueryHandlerFactory.EVENTS)) {
            val payload = try {
                MediaCCCListingPayloadDecoder.decodeEventSearch(
                    MediaCCCParsingHelper.fetchJsonBody(downloader, url, extractorLocalization),
                )
            } catch (error: MediaCCCListingParseException) {
                throw ExtractionException("Could not parse API response from $url", error)
            }
            eventResults = payload.events
                .filter { it.releaseDate != null }
                .map(::MediaCCCStreamInfoItemExtractor)
        }
        if (includes(filters, MediaCCCSearchQueryHandlerFactory.CONFERENCES)) {
            conferenceResults = MediaCCCConferenceKiosk(
                service,
                MediaCCCConferencesListLinkHandlerFactory.getInstance().fromId("conferences"),
                MediaCCCConferenceKiosk.KIOSK_ID,
            ).also { it.onFetchPage(downloader) }
        }
    }

    private fun addMatchingConferences(
        items: MultiInfoItemsCollector,
        conferences: List<ChannelInfoItem>,
    ) {
        val query = searchString.lowercase(Locale.ROOT)
        for (conference in conferences) {
            val name = conference.name ?: continue
            if (!name.lowercase(Locale.ROOT).contains(query)) continue
            items.commit(asExtractor(conference))
        }
    }

    private fun asExtractor(item: ChannelInfoItem): ChannelInfoItemExtractor =
        object : ChannelInfoItemExtractor {
            override fun getDescription(): String? = item.description
            override fun getSubscriberCount(): Long = item.subscriberCount
            override fun getStreamCount(): Long = item.streamCount
            override fun isVerified(): Boolean = item.isVerified
            override fun getName(): String = item.name
            override fun getUrl(): String = item.url
            override fun getThumbnails(): List<Image> = item.thumbnails
        }

    private fun includes(filters: List<String>, wanted: String): Boolean =
        filters.isEmpty() || MediaCCCSearchQueryHandlerFactory.ALL in filters || wanted in filters
}
