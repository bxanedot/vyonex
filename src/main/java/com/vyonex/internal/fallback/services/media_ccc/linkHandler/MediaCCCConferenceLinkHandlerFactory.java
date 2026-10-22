// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.net.URI;
import java.util.List;

public final class MediaCCCConferenceLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final MediaCCCConferenceLinkHandlerFactory INSTANCE =
            new MediaCCCConferenceLinkHandlerFactory();

    public static final String CONFERENCE_API_ENDPOINT =
            "https://api.media.ccc.de/public/conferences/";
    public static final String CONFERENCE_PATH = "https://media.ccc.de/c/";

    private MediaCCCConferenceLinkHandlerFactory() { }

    public static MediaCCCConferenceLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getUrl(final String id, final List<String> contentFilter, final String sortFilter) {
        return CONFERENCE_PATH + id;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        final URI uri = MediaCCCUrl.parse(url);
        final List<String> path = MediaCCCUrl.pathSegments(uri);
        if ((MediaCCCUrl.hasHost(uri, "api.media.ccc.de")
                || MediaCCCUrl.hasHost(uri, "media.ccc.de")) && path.size() == 3
                && "public".equals(path.get(0)) && "conferences".equals(path.get(1))) {
            return path.get(2);
        }
        if (MediaCCCUrl.hasHost(uri, "media.ccc.de") && path.size() == 2
                && ("b".equals(path.get(0)) || "c".equals(path.get(0)))) {
            return path.get(1);
        }
        throw new ParsingException("Unsupported media.ccc.de conference URL: " + url);
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        try {
            getId(url);
            return true;
        } catch (ParsingException e) {
            return false;
        }
    }

    @Override
    public String[] getAvailableContentFilter() { return new String[] { ChannelTabs.VIDEOS }; }
}
