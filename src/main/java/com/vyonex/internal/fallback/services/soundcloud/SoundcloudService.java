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

public class SoundcloudService extends StreamingService {

    public SoundcloudService(final int id) {
        super(id, "SoundCloud", EnumSet.of(AUDIO, COMMENTS));
    }

    @Override
    public String getBaseUrl() {
        return "https://soundcloud.com";
    }

    @Override
    public SearchQueryHandlerFactory getSearchQHFactory() {
        return SoundcloudSearchQueryHandlerFactory.getInstance();
    }

    @Override
    public LinkHandlerFactory getStreamLHFactory() {
        return SoundcloudStreamLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getChannelLHFactory() {
        return SoundcloudChannelLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getChannelTabLHFactory() {
        return SoundcloudChannelTabLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getPlaylistLHFactory() {
        return SoundcloudPlaylistLinkHandlerFactory.getInstance();
    }

    @Override
    public List<ContentCountry> getSupportedCountries() {
        // Country selector here: https://soundcloud.com/charts/top?genre=all-music
        return ContentCountry.listFrom(
                "AU", "CA", "DE", "FR", "GB", "IE", "NL", "NZ", "US"
        );
    }

    @Override
    public StreamExtractor getStreamExtractor(final LinkHandler linkHandler) {
        return new SoundcloudStreamExtractor(this, linkHandler);
    }

    @Override
    public ChannelExtractor getChannelExtractor(final ListLinkHandler linkHandler) {
        return new SoundcloudChannelExtractor(this, linkHandler);
    }

    @Override
    public ChannelTabExtractor getChannelTabExtractor(final ListLinkHandler linkHandler) {
        return new SoundcloudChannelTabExtractor(this, linkHandler);
    }

    @Override
    public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler linkHandler) {
        return new SoundcloudPlaylistExtractor(this, linkHandler);
    }

    @Override
    public SearchExtractor getSearchExtractor(final SearchQueryHandler queryHandler) {
        return new SoundcloudSearchExtractor(this, queryHandler);
    }

    @Override
    public SoundcloudSuggestionExtractor getSuggestionExtractor() {
        return new SoundcloudSuggestionExtractor(this);
    }

    @Override
    public KioskList getKioskList() throws ExtractionException {
        final KioskList list = new KioskList(this);

        final SoundcloudChartsLinkHandlerFactory h =
                SoundcloudChartsLinkHandlerFactory.getInstance();
        final KioskList.KioskExtractorFactory chartsFactory = (streamingService, url, id) ->
                new SoundcloudChartsExtractor(SoundcloudService.this,
                        h.fromUrl(url), id);

        // add kiosks here e.g.:
        try {
            list.addKioskEntry(chartsFactory, h, "New & hot");
            list.setDefaultKiosk("New & hot");
        } catch (final Exception e) {
            throw new ExtractionException(e);
        }

        return list;
    }

    @Override
    public SubscriptionExtractor getSubscriptionExtractor() {
        return new SoundcloudSubscriptionExtractor(this);
    }

    @Override
    public ListLinkHandlerFactory getCommentsLHFactory() {
        return SoundcloudCommentsLinkHandlerFactory.getInstance();
    }

    @Override
    public CommentsExtractor getCommentsExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {
        return new SoundcloudCommentsExtractor(this, linkHandler);
    }
}
