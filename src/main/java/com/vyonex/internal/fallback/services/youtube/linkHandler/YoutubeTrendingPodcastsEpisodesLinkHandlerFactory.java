// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;

public final class YoutubeTrendingPodcastsEpisodesLinkHandlerFactory extends ListLinkHandlerFactory {
    public static final String KIOSK_ID = "trending_podcasts_episodes";
    public static final YoutubeTrendingPodcastsEpisodesLinkHandlerFactory INSTANCE =
            new YoutubeTrendingPodcastsEpisodesLinkHandlerFactory();
    private static final String PATH = "/podcasts/popularepisodes";

    private YoutubeTrendingPodcastsEpisodesLinkHandlerFactory() { }

    @Override public String getUrl(final String id, final List<String> filters, final String sort) {
        return "https://www.youtube.com" + PATH;
    }
    @Override public String getId(final String url) { return KIOSK_ID; }
    @Override public boolean onAcceptUrl(final String value) {
        return YoutubeRouteSupport.hasPath(YoutubeRouteSupport.parse(value), PATH);
    }
}
