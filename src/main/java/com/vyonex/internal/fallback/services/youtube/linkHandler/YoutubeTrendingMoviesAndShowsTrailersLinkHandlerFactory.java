// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;

public final class YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory
        extends ListLinkHandlerFactory {
    public static final String KIOSK_ID = "trending_movies_and_shows";
    public static final YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory INSTANCE =
            new YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory();
    private static final String PATH = "/charts/TrendingTrailers";

    private YoutubeTrendingMoviesAndShowsTrailersLinkHandlerFactory() { }

    @Override public String getUrl(final String id, final List<String> filters, final String sort) {
        return "https://charts.youtube.com" + PATH;
    }
    @Override public String getId(final String url) { return KIOSK_ID; }
    @Override public boolean onAcceptUrl(final String value) {
        return YoutubeRouteSupport.isChartsPath(YoutubeRouteSupport.parse(value), PATH);
    }
}
