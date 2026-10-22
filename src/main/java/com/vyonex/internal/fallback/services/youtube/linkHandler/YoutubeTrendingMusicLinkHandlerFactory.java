// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;

public final class YoutubeTrendingMusicLinkHandlerFactory extends ListLinkHandlerFactory {
    public static final String KIOSK_ID = "trending_music";
    public static final YoutubeTrendingMusicLinkHandlerFactory INSTANCE =
            new YoutubeTrendingMusicLinkHandlerFactory();
    private static final String PATH = "/charts/TrendingVideos";

    private YoutubeTrendingMusicLinkHandlerFactory() { }

    @Override public String getUrl(final String id, final List<String> filters, final String sort) {
        return "https://charts.youtube.com" + PATH + "/RightNow";
    }
    @Override public String getId(final String url) { return KIOSK_ID; }
    @Override public boolean onAcceptUrl(final String value) {
        final var url = YoutubeRouteSupport.parse(value);
        return YoutubeRouteSupport.isChartsHost(url) && url.getPath().startsWith(PATH);
    }
}
