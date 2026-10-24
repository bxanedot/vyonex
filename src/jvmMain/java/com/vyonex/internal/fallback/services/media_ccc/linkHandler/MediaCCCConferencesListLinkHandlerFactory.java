// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

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
        final MediaCCCUrl.Parsed uri = MediaCCCUrl.INSTANCE.parse(value);
        if (uri == null) return false;
        final boolean mediaHost = MediaCCCUrl.INSTANCE.hasHost(uri, "media.ccc.de");
        final boolean apiHost = MediaCCCUrl.INSTANCE.hasHost(uri, "api.media.ccc.de");
        return "https".equals(uri.getScheme()) && !uri.getHasQuery()
                && ((mediaHost && ("/b/conferences".equals(uri.getPath())
                || "/public/conferences".equals(uri.getPath())))
                || (apiHost && "/public/conferences".equals(uri.getPath())));
    }
}
