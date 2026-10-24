// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;

public final class YoutubeTrendingLinkHandlerFactory extends ListLinkHandlerFactory {
    public static final YoutubeTrendingLinkHandlerFactory INSTANCE = new YoutubeTrendingLinkHandlerFactory();

    private YoutubeTrendingLinkHandlerFactory() { }

    @Override public String getUrl(final String id, final List<String> filters, final String sort) {
        return "https://www.youtube.com/feed/trending";
    }
    @Override public String getId(final String url) { return "Trending"; }
    @Override public boolean onAcceptUrl(final String value) {
        return YoutubeRouteSupport.hasPath(YoutubeRouteSupport.parse(value), "/feed/trending");
    }
}
