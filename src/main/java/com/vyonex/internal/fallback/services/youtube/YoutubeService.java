// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import static com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.AUDIO;
import static com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.COMMENTS;
import static com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.LIVE;
import static com.vyonex.internal.fallback.StreamingService.ServiceInfo.MediaCapability.VIDEO;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.comments.CommentsExtractor;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.feed.FeedExtractor;
import com.vyonex.internal.fallback.kiosk.KioskList;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.linkhandler.ReadyChannelTabListLinkHandler;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.playlist.PlaylistExtractor;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeChannelExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeChannelTabExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeCommentsExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeFeedExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeMixPlaylistExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeMusicSearchExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubePlaylistExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeSearchExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeStreamExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeSubscriptionExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeSuggestionExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.kiosk.YoutubeDesktopBaseKioskExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.kiosk.YoutubeChartsBaseKioskExtractor;
import com.vyonex.internal.fallback.services.youtube.extractors.kiosk.YoutubeTrendingExtractor;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeChannelTabLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeCommentsLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeLiveLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeTrendingGamingVideosLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeTrendingLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeTrendingMusicLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeTrendingPodcastsEpisodesLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.subscription.SubscriptionExtractor;
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor;

import java.util.EnumSet;
import java.util.List;

public final class YoutubeService extends StreamingService {
    private static final List<Localization> LANGUAGES = Localization.listFrom("en-GB");
    private static final List<ContentCountry> COUNTRIES = ContentCountry.listFrom(
            "DZ", "AR", "AU", "AT", "AZ", "BH", "BD", "BY", "BE", "BO", "BA", "BR", "BG", "KH",
            "CA", "CL", "CO", "CR", "HR", "CY", "CZ", "DK", "DO", "EC", "EG", "SV", "EE", "FI",
            "FR", "GE", "DE", "GH", "GR", "GT", "HN", "HK", "HU", "IS", "IN", "ID", "IQ", "IE",
            "IL", "IT", "JM", "JP", "JO", "KZ", "KE", "KW", "LA", "LV", "LB", "LY", "LI", "LT",
            "LU", "MY", "MT", "MX", "ME", "MA", "NP", "NL", "NZ", "NI", "NG", "MK", "NO", "OM",
            "PK", "PA", "PG", "PY", "PE", "PH", "PL", "PT", "PR", "QA", "RO", "RU", "SA", "SN",
            "RS", "SG", "SK", "SI", "ZA", "KR", "ES", "LK", "SE", "CH", "TW", "TZ", "TH", "TN",
            "TR", "UG", "UA", "AE", "GB", "US", "UY", "VE", "VN", "YE", "ZW");

    public YoutubeService(final int id) { super(id, "YouTube", EnumSet.of(AUDIO, VIDEO, LIVE, COMMENTS)); }

    @Override public String getBaseUrl() { return "https://youtube.com"; }
    @Override public LinkHandlerFactory getStreamLHFactory() {
        return YoutubeStreamLinkHandlerFactory.getInstance();
    }
    @Override public ListLinkHandlerFactory getChannelLHFactory() {
        return YoutubeChannelLinkHandlerFactory.getInstance();
    }
    @Override public ListLinkHandlerFactory getChannelTabLHFactory() {
        return YoutubeChannelTabLinkHandlerFactory.getInstance();
    }
    @Override public ListLinkHandlerFactory getPlaylistLHFactory() {
        return YoutubePlaylistLinkHandlerFactory.getInstance();
    }
    @Override public SearchQueryHandlerFactory getSearchQHFactory() {
        return YoutubeSearchQueryHandlerFactory.getInstance();
    }
    @Override public StreamExtractor getStreamExtractor(final LinkHandler handler) {
        return new YoutubeStreamExtractor(this, handler);
    }
    @Override public ChannelExtractor getChannelExtractor(final ListLinkHandler handler) {
        return new YoutubeChannelExtractor(this, handler);
    }
    @Override public ChannelTabExtractor getChannelTabExtractor(final ListLinkHandler handler) {
        return handler instanceof ReadyChannelTabListLinkHandler
                ? ((ReadyChannelTabListLinkHandler) handler).getChannelTabExtractor(this)
                : new YoutubeChannelTabExtractor(this, handler);
    }
    @Override public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler handler) {
        return YoutubeParsingHelper.isYoutubeMixId(handler.getId())
                ? new YoutubeMixPlaylistExtractor(this, handler)
                : new YoutubePlaylistExtractor(this, handler);
    }
    @Override public SearchExtractor getSearchExtractor(final SearchQueryHandler handler) {
        final List<String> filters = handler.getContentFilters();
        return !filters.isEmpty() && filters.get(0).startsWith("music_")
                ? new YoutubeMusicSearchExtractor(this, handler)
                : new YoutubeSearchExtractor(this, handler);
    }
    @Override public SuggestionExtractor getSuggestionExtractor() {
        return new YoutubeSuggestionExtractor(this);
    }

    @Override public KioskList getKioskList() throws ExtractionException {
        final KioskList kiosks = new KioskList(this);
        final var live = YoutubeLiveLinkHandlerFactory.INSTANCE;
        final var podcasts = YoutubeTrendingPodcastsEpisodesLinkHandlerFactory.INSTANCE;
        final var gaming = YoutubeTrendingGamingVideosLinkHandlerFactory.INSTANCE;
        final var trailers = YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory.INSTANCE;
        final var music = YoutubeTrendingMusicLinkHandlerFactory.INSTANCE;
        final var trending = YoutubeTrendingLinkHandlerFactory.INSTANCE;
        try {
            kiosks.addKioskEntry((service, url, id) -> new YoutubeDesktopBaseKioskExtractor(this,
                    live.fromUrl(url), id, "UC4R8DWoMoI7CAwX8_LjQHig", "EgdsaXZldGFikgEDCKEK"),
                    live, YoutubeLiveLinkHandlerFactory.KIOSK_ID);
            kiosks.addKioskEntry((service, url, id) -> new YoutubeDesktopBaseKioskExtractor(
                    this, podcasts.fromUrl(url), id, "FEpodcasts_destination", "qgcCCAM%3D"),
                    podcasts,
                    YoutubeTrendingPodcastsEpisodesLinkHandlerFactory.KIOSK_ID);
            kiosks.addKioskEntry((service, url, id) -> new YoutubeDesktopBaseKioskExtractor(
                    this, gaming.fromUrl(url), id, "UCOpNcN46UbXVtpKMrmU4Abg",
                    "Egh0cmVuZGluZw%3D%3D"), gaming,
                    YoutubeTrendingGamingVideosLinkHandlerFactory.KIOSK_ID);
            kiosks.addKioskEntry((service, url, id) -> new YoutubeChartsBaseKioskExtractor(
                    this, trailers.fromUrl(url), id, "TRENDING_MOVIES",
                    "Trending Movie Trailers", false), trailers,
                    YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory.KIOSK_ID);
            kiosks.addKioskEntry((service, url, id) -> new YoutubeChartsBaseKioskExtractor(
                    this, music.fromUrl(url), id, "TRENDING_VIDEOS",
                    "Trending Music Videos", true), music,
                    YoutubeTrendingMusicLinkHandlerFactory.KIOSK_ID);
            kiosks.addKioskEntry((service, url, id) -> new YoutubeTrendingExtractor(
                    this, trending.fromUrl(url), id), trending, YoutubeTrendingExtractor.KIOSK_ID);
            kiosks.setDefaultKiosk(YoutubeLiveLinkHandlerFactory.KIOSK_ID);
            return kiosks;
        } catch (final Exception e) {
            throw new ExtractionException("Unable to initialize YouTube kiosks", e);
        }
    }

    @Override public SubscriptionExtractor getSubscriptionExtractor() {
        return new YoutubeSubscriptionExtractor(this);
    }
    @Override public FeedExtractor getFeedExtractor(final String channelUrl) throws ExtractionException {
        return new YoutubeFeedExtractor(this, getChannelLHFactory().fromUrl(channelUrl));
    }
    @Override public ListLinkHandlerFactory getCommentsLHFactory() {
        return YoutubeCommentsLinkHandlerFactory.getInstance();
    }
    @Override public CommentsExtractor getCommentsExtractor(final ListLinkHandler handler) {
        return new YoutubeCommentsExtractor(this, handler);
    }
    @Override public List<Localization> getSupportedLocalizations() { return LANGUAGES; }
    @Override public List<ContentCountry> getSupportedCountries() { return COUNTRIES; }
}
