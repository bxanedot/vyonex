// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp

import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.channel.ChannelExtractor
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor
import com.vyonex.internal.fallback.comments.CommentsExtractor
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.kiosk.KioskList
import com.vyonex.internal.fallback.linkhandler.LinkHandler
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory
import com.vyonex.internal.fallback.linkhandler.ReadyChannelTabListLinkHandler
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory
import com.vyonex.internal.fallback.playlist.PlaylistExtractor
import com.vyonex.internal.fallback.search.SearchExtractor
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampChannelExtractor
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampChannelTabExtractor
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampCommentsExtractor
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampFeaturedExtractor
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampPlaylistExtractor
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampRadioExtractor
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampRadioStreamExtractor
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampSearchExtractor
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampStreamExtractor
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampSuggestionExtractor
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampChannelLinkHandlerFactory
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampChannelTabLinkHandlerFactory
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampCommentsLinkHandlerFactory
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampFeaturedLinkHandlerFactory
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampPlaylistLinkHandlerFactory
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampSearchQueryHandlerFactory
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampStreamLinkHandlerFactory
import com.vyonex.internal.fallback.stream.StreamExtractor
import com.vyonex.internal.fallback.subscription.SubscriptionExtractor
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor
import java.util.EnumSet

public class BandcampService(id: Int) : StreamingService(
    id,
    "Bandcamp",
    EnumSet.of(
        StreamingService.ServiceInfo.MediaCapability.AUDIO,
        StreamingService.ServiceInfo.MediaCapability.COMMENTS,
    ),
) {
    override fun getBaseUrl(): String = BandcampExtractorHelper.BASE_URL

    override fun getStreamLHFactory(): LinkHandlerFactory = BandcampStreamLinkHandlerFactory.getInstance()

    override fun getChannelLHFactory(): ListLinkHandlerFactory =
        BandcampChannelLinkHandlerFactory.getInstance()

    override fun getChannelTabLHFactory(): ListLinkHandlerFactory =
        BandcampChannelTabLinkHandlerFactory.getInstance()

    override fun getPlaylistLHFactory(): ListLinkHandlerFactory =
        BandcampPlaylistLinkHandlerFactory.getInstance()

    override fun getSearchQHFactory(): SearchQueryHandlerFactory =
        BandcampSearchQueryHandlerFactory.getInstance()

    override fun getCommentsLHFactory(): ListLinkHandlerFactory =
        BandcampCommentsLinkHandlerFactory.getInstance()

    override fun getSearchExtractor(query: SearchQueryHandler): SearchExtractor =
        BandcampSearchExtractor(this, query)

    override fun getSuggestionExtractor(): SuggestionExtractor = BandcampSuggestionExtractor(this)

    override fun getSubscriptionExtractor(): SubscriptionExtractor? = null

    @Throws(ExtractionException::class)
    override fun getKioskList(): KioskList {
        val kiosks = KioskList(this)
        val handlers = BandcampFeaturedLinkHandlerFactory.getInstance()
        try {
            kiosks.addKioskEntry(
                { service, url, id -> BandcampFeaturedExtractor(service, handlers.fromUrl(url), id) },
                handlers,
                BandcampFeaturedExtractor.KIOSK_FEATURED,
            )
            kiosks.addKioskEntry(
                { service, url, id -> BandcampRadioExtractor(service, handlers.fromUrl(url), id) },
                handlers,
                BandcampRadioExtractor.KIOSK_RADIO,
            )
        } catch (error: Exception) {
            throw ExtractionException(error)
        }
        kiosks.setDefaultKiosk(BandcampFeaturedExtractor.KIOSK_FEATURED)
        return kiosks
    }

    override fun getChannelExtractor(linkHandler: ListLinkHandler): ChannelExtractor =
        BandcampChannelExtractor(this, linkHandler)

    override fun getChannelTabExtractor(linkHandler: ListLinkHandler): ChannelTabExtractor =
        if (linkHandler is ReadyChannelTabListLinkHandler) {
            linkHandler.getChannelTabExtractor(this)
        } else {
            BandcampChannelTabExtractor(this, linkHandler)
        }

    override fun getPlaylistExtractor(linkHandler: ListLinkHandler): PlaylistExtractor =
        BandcampPlaylistExtractor(this, linkHandler)

    override fun getStreamExtractor(linkHandler: LinkHandler): StreamExtractor =
        if (BandcampExtractorHelper.isRadioUrl(linkHandler.url)) {
            BandcampRadioStreamExtractor(this, linkHandler)
        } else {
            BandcampStreamExtractor(this, linkHandler)
        }

    override fun getCommentsExtractor(linkHandler: ListLinkHandler): CommentsExtractor =
        BandcampCommentsExtractor(this, linkHandler)
}
