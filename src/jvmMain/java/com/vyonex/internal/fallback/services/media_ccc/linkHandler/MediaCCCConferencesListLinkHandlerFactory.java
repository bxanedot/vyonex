// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.net.URI;
import java.util.List;

public final class MediaCCCConferencesListLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final String URL = "https://media.ccc.de/public/conferences";
    private static final MediaCCCConferencesListLinkHandlerFactory INSTANCE =
            new MediaCCCConferencesListLinkHandlerFactory();

    private MediaCCCConferencesListLinkHandlerFactory() { }
    public static MediaCCCConferencesListLinkHandlerFactory getInstance() { return INSTANCE; }
    @Override public String getId(final String url) { return "conferences"; }
    @Override public String getUrl(final String id, final List<String> filters, final String sort) {
        return URL;
    }
    @Override public boolean onAcceptUrl(final String value) {
        try {
            final URI uri = MediaCCCUrl.parse(value);
            final String path = uri.getPath();
            final boolean officialScheme = "https".equalsIgnoreCase(uri.getScheme());
            final boolean mediaHost = MediaCCCUrl.hasHost(uri, "media.ccc.de");
            final boolean apiHost = MediaCCCUrl.hasHost(uri, "api.media.ccc.de");
            return officialScheme && uri.getQuery() == null
                    && ((mediaHost && ("/b/conferences".equals(path)
                    || "/public/conferences".equals(path)))
                    || (apiHost && "/public/conferences".equals(path)));
        } catch (final ParsingException ignored) {
            return false;
        }
    }
}
