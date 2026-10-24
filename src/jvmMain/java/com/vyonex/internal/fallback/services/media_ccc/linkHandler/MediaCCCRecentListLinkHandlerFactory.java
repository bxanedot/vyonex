// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;

public final class MediaCCCRecentListLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final String URL = "https://media.ccc.de/recent";
    private static final MediaCCCRecentListLinkHandlerFactory INSTANCE =
            new MediaCCCRecentListLinkHandlerFactory();

    private MediaCCCRecentListLinkHandlerFactory() { }
    public static MediaCCCRecentListLinkHandlerFactory getInstance() { return INSTANCE; }
    @Override public String getId(final String url) { return "recent"; }
    @Override public String getUrl(final String id, final List<String> filters, final String sort) {
        return URL;
    }
    @Override public boolean onAcceptUrl(final String value) {
        final MediaCCCUrl.Parsed uri = MediaCCCUrl.INSTANCE.parse(value);
        if (uri == null) return false;
        final String path = uri.getPath();
        final boolean recentPath = "/recent".equals(path) || "/recent/".equals(path);
        return MediaCCCUrl.INSTANCE.hasHost(uri, "media.ccc.de") && recentPath
                && !uri.getHasQuery();
    }
}
