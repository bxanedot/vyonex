// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.net.URI;
import java.util.List;

public final class MediaCCCConferencesListLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final MediaCCCConferencesListLinkHandlerFactory INSTANCE =
            new MediaCCCConferencesListLinkHandlerFactory();
    private static final String URL = "https://media.ccc.de/public/conferences";

    private MediaCCCConferencesListLinkHandlerFactory() { }

    public static MediaCCCConferencesListLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getId(final String url) { return "conferences"; }

    @Override
    public String getUrl(final String id, final List<String> contentFilter, final String sortFilter) {
        return URL;
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        try {
            final URI uri = MediaCCCUrl.parse(url);
            final String path = uri.getPath();
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getQuery() == null
                    && ((MediaCCCUrl.hasHost(uri, "media.ccc.de")
                    && ("/b/conferences".equals(path) || "/public/conferences".equals(path)))
                    || (MediaCCCUrl.hasHost(uri, "api.media.ccc.de")
                    && "/public/conferences".equals(path)));
        } catch (ParsingException e) {
            return false;
        }
    }
}
