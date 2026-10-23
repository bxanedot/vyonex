// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.linkHandler;

import com.vyonex.internal.fallback.ServiceList;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class PeertubeSearchQueryHandlerFactory extends SearchQueryHandlerFactory {
    public static final String VIDEOS = "videos";
    public static final String SEPIA_VIDEOS = "sepia_videos";
    public static final String PLAYLISTS = "playlists";
    public static final String CHANNELS = "channels";
    public static final String SEPIA_BASE_URL = "https://sepiasearch.org";
    public static final String SEARCH_ENDPOINT_PLAYLISTS = "/api/v1/search/video-playlists";
    public static final String SEARCH_ENDPOINT_VIDEOS = "/api/v1/search/videos";
    public static final String SEARCH_ENDPOINT_CHANNELS = "/api/v1/search/video-channels";
    private static final PeertubeSearchQueryHandlerFactory INSTANCE =
            new PeertubeSearchQueryHandlerFactory();

    private PeertubeSearchQueryHandlerFactory() { }
    public static PeertubeSearchQueryHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getUrl(final String query, final List<String> filters, final String sortFilter)
            throws ParsingException {
        final boolean remoteSearch = !filters.isEmpty() && filters.get(0).startsWith("sepia_");
        final String baseUrl = remoteSearch ? SEPIA_BASE_URL : ServiceList.PeerTube.getBaseUrl();
        return getUrl(query, filters, sortFilter, baseUrl);
    }

    @Override
    public String getUrl(final String query, final List<String> filters, final String sortFilter,
                         final String baseUrl) {
        final String kind = filters.isEmpty() ? VIDEOS : filters.get(0);
        final String endpoint = switch (kind) {
            case VIDEOS, SEPIA_VIDEOS -> SEARCH_ENDPOINT_VIDEOS;
            case CHANNELS -> SEARCH_ENDPOINT_CHANNELS;
            case PLAYLISTS -> SEARCH_ENDPOINT_PLAYLISTS;
            default -> SEARCH_ENDPOINT_PLAYLISTS;
        };
        return baseUrl + endpoint + "?search=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    @Override public String[] getAvailableContentFilter() {
        return new String[] { VIDEOS, PLAYLISTS, CHANNELS, SEPIA_VIDEOS };
    }
}
