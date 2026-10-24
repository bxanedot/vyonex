// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud;

import static com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.AUDIO;
import static com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.COMMENTS;

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
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.playlist.PlaylistExtractor;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudChannelExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudChannelTabExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudChartsExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudCommentsExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudPlaylistExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudSearchExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudStreamExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudSubscriptionExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudSuggestionExtractor;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudChannelTabLinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudChartsLinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudCommentsLinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudPlaylistLinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudSearchQueryHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.subscription.SubscriptionExtractor;

import java.util.EnumSet;
import java.util.List;

public final class SoundcloudService extends StreamingService {
    public SoundcloudService(final int id) { super(id, "SoundCloud", EnumSet.of(AUDIO, COMMENTS)); }

    @Override public String getBaseUrl() { return "https://soundcloud.com"; }
    @Override public SearchQueryHandlerFactory getSearchQHFactory() {
        return SoundcloudSearchQueryHandlerFactory.getInstance();
    }
    @Override public LinkHandlerFactory getStreamLHFactory() {
        return SoundcloudStreamLinkHandlerFactory.getInstance();
    }
    @Override public ListLinkHandlerFactory getChannelLHFactory() {
        return SoundcloudChannelLinkHandlerFactory.getInstance();
    }
    @Override public ListLinkHandlerFactory getChannelTabLHFactory() {
        return SoundcloudChannelTabLinkHandlerFactory.getInstance();
    }
    @Override public ListLinkHandlerFactory getPlaylistLHFactory() {
        return SoundcloudPlaylistLinkHandlerFactory.getInstance();
    }
    @Override public List<ContentCountry> getSupportedCountries() {
        return ContentCountry.listFrom("AU", "CA", "DE", "FR", "GB", "IE", "NL", "NZ", "US");
    }
    @Override public StreamExtractor getStreamExtractor(final LinkHandler handler) {
        return new SoundcloudStreamExtractor(this, handler);
    }
    @Override public ChannelExtractor getChannelExtractor(final ListLinkHandler handler) {
        return new SoundcloudChannelExtractor(this, handler);
    }
    @Override public ChannelTabExtractor getChannelTabExtractor(final ListLinkHandler handler) {
        return new SoundcloudChannelTabExtractor(this, handler);
    }
    @Override public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler handler) {
        return new SoundcloudPlaylistExtractor(this, handler);
    }
    @Override public SearchExtractor getSearchExtractor(final SearchQueryHandler handler) {
        return new SoundcloudSearchExtractor(this, handler);
    }
    @Override public SoundcloudSuggestionExtractor getSuggestionExtractor() {
        return new SoundcloudSuggestionExtractor(this);
    }
    @Override public KioskList getKioskList() throws ExtractionException {
        final KioskList kiosks = new KioskList(this);
        final SoundcloudChartsLinkHandlerFactory links = SoundcloudChartsLinkHandlerFactory.getInstance();
        final KioskList.KioskExtractorFactory charts = (service, url, id) ->
                new SoundcloudChartsExtractor(this, links.fromUrl(url), id);
        try {
            kiosks.addKioskEntry(charts, links, "New & hot");
            kiosks.setDefaultKiosk("New & hot");
            return kiosks;
        } catch (final Exception e) {
            throw new ExtractionException("Unable to initialize SoundCloud charts", e);
        }
    }
    @Override public SubscriptionExtractor getSubscriptionExtractor() {
        return new SoundcloudSubscriptionExtractor(this);
    }
    @Override public ListLinkHandlerFactory getCommentsLHFactory() {
        return SoundcloudCommentsLinkHandlerFactory.getInstance();
    }
    @Override public CommentsExtractor getCommentsExtractor(final ListLinkHandler handler)
            throws ExtractionException {
        return new SoundcloudCommentsExtractor(this, handler);
    }
}
