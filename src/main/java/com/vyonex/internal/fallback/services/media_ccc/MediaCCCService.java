// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc;

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
import com.vyonex.internal.fallback.linkhandler.ReadyChannelTabListLinkHandler;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory;
import com.vyonex.internal.fallback.playlist.PlaylistExtractor;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCChannelTabExtractor;
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCConferenceExtractor;
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCConferenceKiosk;
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCLiveStreamExtractor;
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCLiveStreamKiosk;
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper;
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCRecentKiosk;
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCSearchExtractor;
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCStreamExtractor;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferencesListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCLiveListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCRecentListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCSearchQueryHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.subscription.SubscriptionExtractor;
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor;

import java.util.EnumSet;

import static com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.AUDIO;
import static com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.VIDEO;

public class MediaCCCService extends StreamingService {
    public MediaCCCService(final int id) {
        super(id, "media.ccc.de", EnumSet.of(AUDIO, VIDEO));
    }

    @Override public SearchExtractor getSearchExtractor(final SearchQueryHandler query) {
        return new MediaCCCSearchExtractor(this, query);
    }
    @Override public LinkHandlerFactory getStreamLHFactory() {
        return MediaCCCStreamLinkHandlerFactory.getInstance();
    }
    @Override public ListLinkHandlerFactory getChannelLHFactory() {
        return MediaCCCConferenceLinkHandlerFactory.getInstance();
    }
    @Override public ListLinkHandlerFactory getChannelTabLHFactory() {
        return MediaCCCConferenceLinkHandlerFactory.getInstance();
    }
    @Override public ListLinkHandlerFactory getPlaylistLHFactory() { return null; }
    @Override public SearchQueryHandlerFactory getSearchQHFactory() {
        return MediaCCCSearchQueryHandlerFactory.getInstance();
    }
    @Override public StreamExtractor getStreamExtractor(final LinkHandler link) {
        if (MediaCCCParsingHelper.isLiveStreamId(link.getId())) {
            return new MediaCCCLiveStreamExtractor(this, link);
        }
        return new MediaCCCStreamExtractor(this, link);
    }
    @Override public ChannelExtractor getChannelExtractor(final ListLinkHandler link) {
        return new MediaCCCConferenceExtractor(this, link);
    }
    @Override public ChannelTabExtractor getChannelTabExtractor(final ListLinkHandler link) {
        if (link instanceof ReadyChannelTabListLinkHandler) {
            return ((ReadyChannelTabListLinkHandler) link).getChannelTabExtractor(this);
        }
        return new MediaCCCChannelTabExtractor(this, link, null);
    }
    @Override public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler link) { return null; }
    @Override public SuggestionExtractor getSuggestionExtractor() { return null; }
    @Override public SubscriptionExtractor getSubscriptionExtractor() { return null; }
    @Override public ListLinkHandlerFactory getCommentsLHFactory() { return null; }
    @Override public CommentsExtractor getCommentsExtractor(final ListLinkHandler link) { return null; }
    @Override public String getBaseUrl() { return "https://media.ccc.de"; }

    @Override
    public KioskList getKioskList() throws ExtractionException {
        final KioskList kiosks = new KioskList(this);
        final MediaCCCConferencesListLinkHandlerFactory conferences =
                MediaCCCConferencesListLinkHandlerFactory.getInstance();
        final MediaCCCRecentListLinkHandlerFactory recent =
                MediaCCCRecentListLinkHandlerFactory.getInstance();
        final MediaCCCLiveListLinkHandlerFactory live =
                MediaCCCLiveListLinkHandlerFactory.getInstance();
        try {
            kiosks.addKioskEntry((service, url, id) ->
                            new MediaCCCConferenceKiosk(service, conferences.fromUrl(url), id),
                    conferences, MediaCCCConferenceKiosk.KIOSK_ID);
            kiosks.addKioskEntry((service, url, id) ->
                            new MediaCCCRecentKiosk(service, recent.fromUrl(url), id),
                    recent, MediaCCCRecentKiosk.KIOSK_ID);
            kiosks.addKioskEntry((service, url, id) ->
                            new MediaCCCLiveStreamKiosk(service, live.fromUrl(url), id),
                    live, MediaCCCLiveStreamKiosk.KIOSK_ID);
        } catch (final Exception error) {
            throw new ExtractionException(error);
        }
        kiosks.setDefaultKiosk(MediaCCCRecentKiosk.KIOSK_ID);
        return kiosks;
    }
}
