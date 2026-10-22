// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

// bxane
package com.vyonex.internal.fallback.services.bandcamp;

import static com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.AUDIO;
import static com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.COMMENTS;
import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.BASE_URL;
import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampFeaturedExtractor.FEATURED_API_URL;
import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampFeaturedExtractor.KIOSK_FEATURED;
import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampRadioExtractor.KIOSK_RADIO;
import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampRadioExtractor.RADIO_API_URL;

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
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampChannelExtractor;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampChannelTabExtractor;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampCommentsExtractor;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampFeaturedExtractor;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampPlaylistExtractor;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampRadioExtractor;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampRadioStreamExtractor;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampSearchExtractor;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampStreamExtractor;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampSuggestionExtractor;
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampChannelTabLinkHandlerFactory;
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampCommentsLinkHandlerFactory;
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampFeaturedLinkHandlerFactory;
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampPlaylistLinkHandlerFactory;
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampSearchQueryHandlerFactory;
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.subscription.SubscriptionExtractor;
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor;

import java.util.EnumSet;

public class BandcampService extends StreamingService {

    public BandcampService(final int id) {
        super(id, "Bandcamp", EnumSet.of(AUDIO, COMMENTS));
    }

    @Override
    public String getBaseUrl() {
        return BASE_URL;
    }

    @Override
    public LinkHandlerFactory getStreamLHFactory() {
        return BandcampStreamLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getChannelLHFactory() {
        return BandcampChannelLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getChannelTabLHFactory() {
        return BandcampChannelTabLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getPlaylistLHFactory() {
        return BandcampPlaylistLinkHandlerFactory.getInstance();
    }

    @Override
    public SearchQueryHandlerFactory getSearchQHFactory() {
        return BandcampSearchQueryHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getCommentsLHFactory() {
        return BandcampCommentsLinkHandlerFactory.getInstance();
    }

    @Override
    public SearchExtractor getSearchExtractor(final SearchQueryHandler queryHandler) {
        return new BandcampSearchExtractor(this, queryHandler);
    }

    @Override
    public SuggestionExtractor getSuggestionExtractor() {
        return new BandcampSuggestionExtractor(this);
    }

    @Override
    public SubscriptionExtractor getSubscriptionExtractor() {
        return null;
    }

    @Override
    public KioskList getKioskList() throws ExtractionException {
        final KioskList kioskList = new KioskList(this);
        final ListLinkHandlerFactory h = BandcampFeaturedLinkHandlerFactory.getInstance();

        try {
            kioskList.addKioskEntry(
                    (streamingService, url, kioskId) -> new BandcampFeaturedExtractor(
                            BandcampService.this,
                            h.fromUrl(FEATURED_API_URL),
                            kioskId
                    ),
                    h,
                    KIOSK_FEATURED
            );

            kioskList.addKioskEntry(
                    (streamingService, url, kioskId) -> new BandcampRadioExtractor(
                            BandcampService.this,
                            h.fromUrl(RADIO_API_URL),
                            kioskId
                    ),
                    h,
                    KIOSK_RADIO
            );

            kioskList.setDefaultKiosk(KIOSK_FEATURED);

        } catch (final Exception e) {
            throw new ExtractionException(e);
        }

        return kioskList;
    }

    @Override
    public ChannelExtractor getChannelExtractor(final ListLinkHandler linkHandler) {
        return new BandcampChannelExtractor(this, linkHandler);
    }

    @Override
    public ChannelTabExtractor getChannelTabExtractor(final ListLinkHandler linkHandler) {
        if (linkHandler instanceof ReadyChannelTabListLinkHandler) {
            return ((ReadyChannelTabListLinkHandler) linkHandler).getChannelTabExtractor(this);
        } else {
            return new BandcampChannelTabExtractor(this, linkHandler);
        }
    }

    @Override
    public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler linkHandler) {
        return new BandcampPlaylistExtractor(this, linkHandler);
    }

    @Override
    public StreamExtractor getStreamExtractor(final LinkHandler linkHandler) {
        if (BandcampExtractorHelper.isRadioUrl(linkHandler.getUrl())) {
            return new BandcampRadioStreamExtractor(this, linkHandler);
        }
        return new BandcampStreamExtractor(this, linkHandler);
    }

    @Override
    public CommentsExtractor getCommentsExtractor(final ListLinkHandler linkHandler) {
        return new BandcampCommentsExtractor(this, linkHandler);
    }
}
