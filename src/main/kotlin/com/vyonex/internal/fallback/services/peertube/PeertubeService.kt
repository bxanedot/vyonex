// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube

import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.COMMENTS
import com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.VIDEO
import com.vyonex.internal.fallback.channel.ChannelExtractor
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor
import com.vyonex.internal.fallback.comments.CommentsExtractor
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.kiosk.KioskList
import com.vyonex.internal.fallback.linkhandler.LinkHandler
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory
import com.vyonex.internal.fallback.playlist.PlaylistExtractor
import com.vyonex.internal.fallback.search.SearchExtractor
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeAccountExtractor
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeChannelExtractor
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeChannelTabExtractor
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeCommentsExtractor
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubePlaylistExtractor
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeSearchExtractor
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeStreamExtractor
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeSuggestionExtractor
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeTrendingExtractor
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeCommentsLinkHandlerFactory
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubePlaylistLinkHandlerFactory
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeSearchQueryHandlerFactory
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeStreamLinkHandlerFactory
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeTrendingLinkHandlerFactory
import com.vyonex.internal.fallback.stream.StreamExtractor
import com.vyonex.internal.fallback.subscription.SubscriptionExtractor
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor
import java.util.EnumSet

public class PeertubeService @JvmOverloads constructor(
    id: Int,
    private var currentInstance: PeertubeInstance = PeertubeInstance.DEFAULT_INSTANCE,
) : StreamingService(id, "PeerTube", EnumSet.of(VIDEO, COMMENTS)) {
    override fun getStreamLHFactory(): LinkHandlerFactory =
        PeertubeStreamLinkHandlerFactory.getInstance()

    override fun getChannelLHFactory(): ListLinkHandlerFactory =
        PeertubeChannelLinkHandlerFactory.getInstance()

    override fun getChannelTabLHFactory(): ListLinkHandlerFactory =
        PeertubeChannelTabLinkHandlerFactory.getInstance()

    override fun getPlaylistLHFactory(): ListLinkHandlerFactory =
        PeertubePlaylistLinkHandlerFactory.getInstance()

    override fun getSearchQHFactory(): SearchQueryHandlerFactory =
        PeertubeSearchQueryHandlerFactory.getInstance()

    override fun getCommentsLHFactory(): ListLinkHandlerFactory =
        PeertubeCommentsLinkHandlerFactory.getInstance()

    override fun getSearchExtractor(query: SearchQueryHandler): SearchExtractor {
        val filters = query.contentFilters
        val sepia = filters.isNotEmpty() && filters[0].startsWith("sepia_")
        return PeertubeSearchExtractor(this, query, sepia)
    }

    override fun getSuggestionExtractor(): SuggestionExtractor = PeertubeSuggestionExtractor(this)

    override fun getSubscriptionExtractor(): SubscriptionExtractor? = null

    @Throws(ExtractionException::class)
    override fun getChannelExtractor(link: ListLinkHandler): ChannelExtractor =
        if (link.id.startsWith("video-channels/")) PeertubeChannelExtractor(this, link)
        else PeertubeAccountExtractor(this, link)

    @Throws(ExtractionException::class)
    override fun getChannelTabExtractor(link: ListLinkHandler): ChannelTabExtractor =
        PeertubeChannelTabExtractor(this, link)

    @Throws(ExtractionException::class)
    override fun getPlaylistExtractor(link: ListLinkHandler): PlaylistExtractor =
        PeertubePlaylistExtractor(this, link)

    @Throws(ExtractionException::class)
    override fun getStreamExtractor(link: LinkHandler): StreamExtractor =
        PeertubeStreamExtractor(this, link)

    @Throws(ExtractionException::class)
    override fun getCommentsExtractor(link: ListLinkHandler): CommentsExtractor =
        PeertubeCommentsExtractor(this, link)

    override fun getBaseUrl(): String = currentInstance.getUrl()

    public fun getInstance(): PeertubeInstance = currentInstance

    public fun setInstance(instance: PeertubeInstance) {
        currentInstance = instance
    }

    @Throws(ExtractionException::class)
    override fun getKioskList(): KioskList {
        val handlers = PeertubeTrendingLinkHandlerFactory.getInstance()
        val factory = KioskList.KioskExtractorFactory { _, _, id ->
            PeertubeTrendingExtractor(this, handlers.fromId(id), id)
        }
        val kiosks = KioskList(this)
        try {
            kiosks.addKioskEntry(factory, handlers, PeertubeTrendingLinkHandlerFactory.KIOSK_TRENDING)
            kiosks.addKioskEntry(factory, handlers, PeertubeTrendingLinkHandlerFactory.KIOSK_MOST_LIKED)
            kiosks.addKioskEntry(factory, handlers, PeertubeTrendingLinkHandlerFactory.KIOSK_RECENT)
            kiosks.addKioskEntry(factory, handlers, PeertubeTrendingLinkHandlerFactory.KIOSK_LOCAL)
            kiosks.setDefaultKiosk(PeertubeTrendingLinkHandlerFactory.KIOSK_TRENDING)
        } catch (error: Exception) {
            throw ExtractionException(error)
        }
        return kiosks
    }
}
