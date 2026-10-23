// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc

import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.AUDIO
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
import com.vyonex.internal.fallback.linkhandler.ReadyChannelTabListLinkHandler
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory
import com.vyonex.internal.fallback.playlist.PlaylistExtractor
import com.vyonex.internal.fallback.search.SearchExtractor
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCChannelTabExtractor
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCConferenceExtractor
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCConferenceKiosk
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCLiveStreamExtractor
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCLiveStreamKiosk
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCRecentKiosk
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCSearchExtractor
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCStreamExtractor
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferencesListLinkHandlerFactory
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCLiveListLinkHandlerFactory
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCRecentListLinkHandlerFactory
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCSearchQueryHandlerFactory
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCStreamLinkHandlerFactory
import com.vyonex.internal.fallback.stream.StreamExtractor
import com.vyonex.internal.fallback.subscription.SubscriptionExtractor
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor
import java.util.EnumSet

public class MediaCCCService(id: Int) : StreamingService(id, "media.ccc.de", EnumSet.of(AUDIO, VIDEO)) {
    override fun getSearchExtractor(query: SearchQueryHandler): SearchExtractor =
        MediaCCCSearchExtractor(this, query)

    override fun getStreamLHFactory(): LinkHandlerFactory = MediaCCCStreamLinkHandlerFactory.getInstance()

    override fun getChannelLHFactory(): ListLinkHandlerFactory =
        MediaCCCConferenceLinkHandlerFactory.getInstance()

    override fun getChannelTabLHFactory(): ListLinkHandlerFactory =
        MediaCCCConferenceLinkHandlerFactory.getInstance()

    override fun getPlaylistLHFactory(): ListLinkHandlerFactory? = null

    override fun getSearchQHFactory(): SearchQueryHandlerFactory =
        MediaCCCSearchQueryHandlerFactory.getInstance()

    override fun getStreamExtractor(link: LinkHandler): StreamExtractor =
        if (MediaCCCParsingHelper.isLiveStreamId(link.id)) {
            MediaCCCLiveStreamExtractor(this, link)
        } else {
            MediaCCCStreamExtractor(this, link)
        }

    override fun getChannelExtractor(link: ListLinkHandler): ChannelExtractor =
        MediaCCCConferenceExtractor(this, link)

    override fun getChannelTabExtractor(link: ListLinkHandler): ChannelTabExtractor =
        if (link is ReadyChannelTabListLinkHandler) {
            link.getChannelTabExtractor(this)
        } else {
            MediaCCCChannelTabExtractor(this, link, null)
        }

    override fun getPlaylistExtractor(link: ListLinkHandler): PlaylistExtractor? = null

    override fun getSuggestionExtractor(): SuggestionExtractor? = null

    override fun getSubscriptionExtractor(): SubscriptionExtractor? = null

    override fun getCommentsLHFactory(): ListLinkHandlerFactory? = null

    override fun getCommentsExtractor(link: ListLinkHandler): CommentsExtractor? = null

    override fun getBaseUrl(): String = "https://media.ccc.de"

    @Throws(ExtractionException::class)
    override fun getKioskList(): KioskList {
        val kiosks = KioskList(this)
        val conferences = MediaCCCConferencesListLinkHandlerFactory.getInstance()
        val recent = MediaCCCRecentListLinkHandlerFactory.getInstance()
        val live = MediaCCCLiveListLinkHandlerFactory.getInstance()
        try {
            kiosks.addKioskEntry(
                { service, url, id -> MediaCCCConferenceKiosk(service, conferences.fromUrl(url), id) },
                conferences,
                MediaCCCConferenceKiosk.KIOSK_ID,
            )
            kiosks.addKioskEntry(
                { service, url, id -> MediaCCCRecentKiosk(service, recent.fromUrl(url), id) },
                recent,
                MediaCCCRecentKiosk.KIOSK_ID,
            )
            kiosks.addKioskEntry(
                { service, url, id -> MediaCCCLiveStreamKiosk(service, live.fromUrl(url), id) },
                live,
                MediaCCCLiveStreamKiosk.KIOSK_ID,
            )
        } catch (error: Exception) {
            throw ExtractionException(error)
        }
        kiosks.setDefaultKiosk(MediaCCCRecentKiosk.KIOSK_ID)
        return kiosks
    }
}
