// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors

import com.vyonex.internal.fallback.InfoItem
import com.vyonex.internal.fallback.ListExtractor
import com.vyonex.internal.fallback.MultiInfoItemsCollector
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.services.media_ccc.extractors.infoItems.MediaCCCStreamInfoItemExtractor

public class MediaCCCChannelTabExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
    events: List<MediaCCCStreamInfoItemExtractor>?,
) : ChannelTabExtractor(service, linkHandler) {
    private var events: List<MediaCCCStreamInfoItemExtractor>? = events

    override fun onFetchPage(downloader: Downloader) {
        if (events == null) events = fetchMediaCCCConferenceData(downloader, id).events
    }

    override fun getInitialPage(): ListExtractor.InfoItemsPage<InfoItem> {
        val fetched = events ?: throw IllegalStateException("Conference was not fetched")
        val items = MultiInfoItemsCollector(serviceId)
        fetched.forEach(items::commit)
        return ListExtractor.InfoItemsPage(items, null)
    }

    override fun getPage(page: com.vyonex.internal.fallback.Page): ListExtractor.InfoItemsPage<InfoItem> =
        ListExtractor.InfoItemsPage.emptyPage()
}
