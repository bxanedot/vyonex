// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.net.URI;
import java.util.List;

public final class MediaCCCRecentListLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final MediaCCCRecentListLinkHandlerFactory INSTANCE =
            new MediaCCCRecentListLinkHandlerFactory();
    private static final String URL = "https://media.ccc.de/recent";

    private MediaCCCRecentListLinkHandlerFactory() { }

    public static MediaCCCRecentListLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getId(final String url) { return "recent"; }

    @Override
    public boolean onAcceptUrl(final String url) {
        try {
            final URI uri = MediaCCCUrl.parse(url);
            final String path = uri.getPath();
            return MediaCCCUrl.hasHost(uri, "media.ccc.de")
                    && ("/recent".equals(path) || "/recent/".equals(path))
                    && uri.getQuery() == null;
        } catch (ParsingException e) {
            return false;
        }
    }

    @Override
    public String getUrl(final String id, final List<String> contentFilter, final String sortFilter) {
        return URL;
    }
}
