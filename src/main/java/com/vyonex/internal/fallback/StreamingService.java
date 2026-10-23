// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.comments.CommentsExtractor;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.feed.FeedExtractor;
import com.vyonex.internal.fallback.kiosk.KioskList;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.fallback.localization.TimeAgoPatternsManager;
import com.vyonex.internal.fallback.playlist.PlaylistExtractor;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.subscription.SubscriptionExtractor;
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor;
import com.vyonex.internal.fallback.utils.Utils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class StreamingService {
    public static class ServiceInfo {
        private final String name;
        private final Set<MediaCapability> mediaCapabilities;

        public ServiceInfo(final String name, final Set<MediaCapability> mediaCapabilities) {
            this.name = name;
            this.mediaCapabilities = mediaCapabilities;
        }

        public String getName() { return name; }
        public Set<MediaCapability> getMediaCapabilities() { return mediaCapabilities; }

        public enum MediaCapability { AUDIO, VIDEO, LIVE, COMMENTS }
    }

    public enum LinkType { NONE, STREAM, CHANNEL, PLAYLIST }

    private final int serviceId;
    private final ServiceInfo serviceInfo;

    public StreamingService(final int id, final String name,
                            final Set<ServiceInfo.MediaCapability> capabilities) {
        serviceId = id;
        serviceInfo = new ServiceInfo(name, capabilities);
    }

    public final int getServiceId() { return serviceId; }
    public ServiceInfo getServiceInfo() { return serviceInfo; }
    @Override public String toString() { return serviceId + ":" + serviceInfo.getName(); }

    public abstract String getBaseUrl();
    public abstract SearchQueryHandlerFactory getSearchQHFactory();
    public abstract LinkHandlerFactory getStreamLHFactory();
    public abstract ListLinkHandlerFactory getChannelLHFactory();
    public abstract ListLinkHandlerFactory getChannelTabLHFactory();
    public abstract ListLinkHandlerFactory getPlaylistLHFactory();
    public abstract ListLinkHandlerFactory getCommentsLHFactory();

    public abstract SearchExtractor getSearchExtractor(SearchQueryHandler queryHandler);
    public abstract SuggestionExtractor getSuggestionExtractor();
    public abstract SubscriptionExtractor getSubscriptionExtractor();
    public abstract KioskList getKioskList() throws ExtractionException;
    public abstract ChannelExtractor getChannelExtractor(ListLinkHandler linkHandler)
            throws ExtractionException;
    public abstract ChannelTabExtractor getChannelTabExtractor(ListLinkHandler linkHandler)
            throws ExtractionException;
    public abstract PlaylistExtractor getPlaylistExtractor(ListLinkHandler linkHandler)
            throws ExtractionException;
    public abstract StreamExtractor getStreamExtractor(LinkHandler linkHandler)
            throws ExtractionException;
    public abstract CommentsExtractor getCommentsExtractor(ListLinkHandler linkHandler)
            throws ExtractionException;

    @Nullable public FeedExtractor getFeedExtractor(final String url) throws ExtractionException {
        return null;
    }

    public SearchExtractor getSearchExtractor(final String query,
                                              final List<String> contentFilter,
                                              final String sortFilter) throws ExtractionException {
        return getSearchExtractor(getSearchQHFactory().fromQuery(query, contentFilter, sortFilter));
    }

    public ChannelExtractor getChannelExtractor(final String id,
                                                final List<String> contentFilter,
                                                final String sortFilter) throws ExtractionException {
        return getChannelExtractor(getChannelLHFactory().fromQuery(id, contentFilter, sortFilter));
    }

    public PlaylistExtractor getPlaylistExtractor(final String id,
                                                  final List<String> contentFilter,
                                                  final String sortFilter) throws ExtractionException {
        return getPlaylistExtractor(getPlaylistLHFactory().fromQuery(id, contentFilter, sortFilter));
    }

    public SearchExtractor getSearchExtractor(final String query) throws ExtractionException {
        return getSearchExtractor(getSearchQHFactory().fromQuery(query));
    }

    public ChannelExtractor getChannelExtractor(final String url) throws ExtractionException {
        return getChannelExtractor(getChannelLHFactory().fromUrl(url));
    }

    public ChannelTabExtractor getChannelTabExtractorFromId(final String id, final String tab)
            throws ExtractionException {
        return getChannelTabExtractor(getChannelTabLHFactory()
                .fromQuery(id, Collections.singletonList(tab), ""));
    }

    public ChannelTabExtractor getChannelTabExtractorFromIdAndBaseUrl(
            final String id, final String tab, final String baseUrl) throws ExtractionException {
        return getChannelTabExtractor(getChannelTabLHFactory()
                .fromQuery(id, Collections.singletonList(tab), "", baseUrl));
    }

    public PlaylistExtractor getPlaylistExtractor(final String url) throws ExtractionException {
        return getPlaylistExtractor(getPlaylistLHFactory().fromUrl(url));
    }

    public StreamExtractor getStreamExtractor(final String url) throws ExtractionException {
        return getStreamExtractor(getStreamLHFactory().fromUrl(url));
    }

    public CommentsExtractor getCommentsExtractor(final String url) throws ExtractionException {
        final ListLinkHandlerFactory factory = getCommentsLHFactory();
        return factory == null ? null : getCommentsExtractor(factory.fromUrl(url));
    }

    public final LinkType getLinkTypeByUrl(final String url) throws ParsingException {
        final String candidate = Utils.followGoogleRedirectIfNeeded(url);
        final LinkHandlerFactory streams = getStreamLHFactory();
        if (streams != null && streams.acceptUrl(candidate)) return LinkType.STREAM;

        final ListLinkHandlerFactory channels = getChannelLHFactory();
        if (channels != null && channels.acceptUrl(candidate)) return LinkType.CHANNEL;

        final ListLinkHandlerFactory playlists = getPlaylistLHFactory();
        if (playlists != null && playlists.acceptUrl(candidate)) return LinkType.PLAYLIST;
        return LinkType.NONE;
    }

    public List<Localization> getSupportedLocalizations() {
        return Collections.singletonList(Localization.DEFAULT);
    }

    public List<ContentCountry> getSupportedCountries() {
        return Collections.singletonList(ContentCountry.DEFAULT);
    }

    public Localization getLocalization() {
        final Localization preferred = VyonEx.getPreferredLocalization();
        final List<Localization> supported = getSupportedLocalizations();
        if (supported.contains(preferred)) return preferred;
        for (final Localization option : supported) {
            if (option.getLanguageCode().equals(preferred.getLanguageCode())) return option;
        }
        return Localization.DEFAULT;
    }

    public ContentCountry getContentCountry() {
        final ContentCountry preferred = VyonEx.getPreferredContentCountry();
        return getSupportedCountries().contains(preferred) ? preferred : ContentCountry.DEFAULT;
    }

    public TimeAgoParser getTimeAgoParser(final Localization localization) {
        TimeAgoParser parser = TimeAgoPatternsManager.getTimeAgoParserFor(localization);
        if (parser == null && !localization.getCountryCode().isEmpty()) {
            parser = TimeAgoPatternsManager.getTimeAgoParserFor(
                    new Localization(localization.getLanguageCode()));
        }
        if (parser == null) {
            throw new IllegalArgumentException("Localization is not supported (\""
                    + localization + "\")");
        }
        return parser;
    }
}
