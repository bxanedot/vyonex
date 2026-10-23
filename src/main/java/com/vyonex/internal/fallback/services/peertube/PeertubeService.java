// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.comments.CommentsExtractor;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.kiosk.KioskList;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory;
import com.vyonex.internal.fallback.playlist.PlaylistExtractor;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeAccountExtractor;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeChannelExtractor;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeChannelTabExtractor;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeCommentsExtractor;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubePlaylistExtractor;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeSearchExtractor;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeStreamExtractor;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeSuggestionExtractor;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeTrendingExtractor;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeCommentsLinkHandlerFactory;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubePlaylistLinkHandlerFactory;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeSearchQueryHandlerFactory;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeTrendingLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.subscription.SubscriptionExtractor;
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor;

import java.util.EnumSet;
import java.util.List;

import static com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.COMMENTS;
import static com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.VIDEO;

public class PeertubeService extends StreamingService {
    private PeertubeInstance instance;

    public PeertubeService(final int id) {
        this(id, PeertubeInstance.DEFAULT_INSTANCE);
    }

    public PeertubeService(final int id, final PeertubeInstance instance) {
        super(id, "PeerTube", EnumSet.of(VIDEO, COMMENTS));
        this.instance = instance;
    }

    @Override public LinkHandlerFactory getStreamLHFactory() {
        return PeertubeStreamLinkHandlerFactory.getInstance();
    }

    @Override public ListLinkHandlerFactory getChannelLHFactory() {
        return PeertubeChannelLinkHandlerFactory.getInstance();
    }

    @Override public ListLinkHandlerFactory getChannelTabLHFactory() {
        return PeertubeChannelTabLinkHandlerFactory.getInstance();
    }

    @Override public ListLinkHandlerFactory getPlaylistLHFactory() {
        return PeertubePlaylistLinkHandlerFactory.getInstance();
    }

    @Override public SearchQueryHandlerFactory getSearchQHFactory() {
        return PeertubeSearchQueryHandlerFactory.getInstance();
    }

    @Override public ListLinkHandlerFactory getCommentsLHFactory() {
        return PeertubeCommentsLinkHandlerFactory.getInstance();
    }

    @Override
    public SearchExtractor getSearchExtractor(final SearchQueryHandler query) {
        final List<String> filters = query.getContentFilters();
        final boolean useSepia = !filters.isEmpty() && filters.get(0).startsWith("sepia_");
        return new PeertubeSearchExtractor(this, query, useSepia);
    }

    @Override public SuggestionExtractor getSuggestionExtractor() {
        return new PeertubeSuggestionExtractor(this);
    }

    @Override public SubscriptionExtractor getSubscriptionExtractor() { return null; }

    @Override
    public ChannelExtractor getChannelExtractor(final ListLinkHandler link)
            throws ExtractionException {
        if (link.getId().startsWith("video-channels/")) return new PeertubeChannelExtractor(this, link);
        return new PeertubeAccountExtractor(this, link);
    }

    @Override
    public ChannelTabExtractor getChannelTabExtractor(final ListLinkHandler link)
            throws ExtractionException {
        return new PeertubeChannelTabExtractor(this, link);
    }

    @Override
    public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler link)
            throws ExtractionException {
        return new PeertubePlaylistExtractor(this, link);
    }

    @Override
    public StreamExtractor getStreamExtractor(final LinkHandler link) throws ExtractionException {
        return new PeertubeStreamExtractor(this, link);
    }

    @Override
    public CommentsExtractor getCommentsExtractor(final ListLinkHandler link)
            throws ExtractionException {
        return new PeertubeCommentsExtractor(this, link);
    }

    @Override public String getBaseUrl() { return instance.getUrl(); }
    public PeertubeInstance getInstance() { return instance; }
    public void setInstance(final PeertubeInstance instance) { this.instance = instance; }

    @Override
    public KioskList getKioskList() throws ExtractionException {
        final PeertubeTrendingLinkHandlerFactory handlers =
                PeertubeTrendingLinkHandlerFactory.getInstance();
        final KioskList.KioskExtractorFactory factory = (service, url, id) ->
                new PeertubeTrendingExtractor(this, handlers.fromId(id), id);
        final KioskList kiosks = new KioskList(this);
        try {
            kiosks.addKioskEntry(factory, handlers, PeertubeTrendingLinkHandlerFactory.KIOSK_TRENDING);
            kiosks.addKioskEntry(factory, handlers, PeertubeTrendingLinkHandlerFactory.KIOSK_MOST_LIKED);
            kiosks.addKioskEntry(factory, handlers, PeertubeTrendingLinkHandlerFactory.KIOSK_RECENT);
            kiosks.addKioskEntry(factory, handlers, PeertubeTrendingLinkHandlerFactory.KIOSK_LOCAL);
            kiosks.setDefaultKiosk(PeertubeTrendingLinkHandlerFactory.KIOSK_TRENDING);
        } catch (final Exception error) {
            throw new ExtractionException(error);
        }
        return kiosks;
    }
}
