// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors

import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.kiosk.KioskExtractor
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.stream.StreamInfoItem
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector
import javax.annotation.Nonnull

public class MediaCCCLiveStreamKiosk(
    service: StreamingService,
    linkHandler: ListLinkHandler,
    kioskId: String,
) : KioskExtractor<StreamInfoItem>(service, linkHandler, kioskId) {
    private lateinit var rooms: List<StreamInfoItemExtractor>

    @Throws(ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        rooms = MediaCCCParsingHelper.getLiveStreams(downloader, extractorLocalization)
            .asSequence()
            .filter { it.isCurrentlyStreaming == true }
            .flatMap { conference ->
                conference.groups.orEmpty().asSequence().flatMap { group ->
                    group.rooms.orEmpty().asSequence().map { room ->
                        MediaCCCLiveStreamKioskExtractor(conference, group.group, room)
                    }
                }
            }
            .toList()
    }

    @Nonnull
    override fun getInitialPage(): InfoItemsPage<StreamInfoItem> {
        val items = StreamInfoItemsCollector(serviceId)
        rooms.forEach(items::commit)
        return InfoItemsPage(items, null)
    }

    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> = InfoItemsPage.emptyPage()

    @Nonnull
    override fun getName(): String = KIOSK_ID

    public companion object {
        public const val KIOSK_ID: String = "live"
    }
}
