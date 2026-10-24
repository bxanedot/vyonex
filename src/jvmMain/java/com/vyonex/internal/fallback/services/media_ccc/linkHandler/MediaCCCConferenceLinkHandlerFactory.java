// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;

public final class MediaCCCConferenceLinkHandlerFactory extends ListLinkHandlerFactory {
    public static final String CONFERENCE_API_ENDPOINT =
            "https://api.media.ccc.de/public/conferences/";
    public static final String CONFERENCE_PATH = "https://media.ccc.de/c/";
    private static final MediaCCCConferenceLinkHandlerFactory INSTANCE =
            new MediaCCCConferenceLinkHandlerFactory();

    private MediaCCCConferenceLinkHandlerFactory() { }
    public static MediaCCCConferenceLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getUrl(final String id, final List<String> filters, final String sort) {
        return CONFERENCE_PATH + id;
    }
    @Override public String getId(final String value) throws ParsingException {
        final MediaCCCUrl.Parsed uri = MediaCCCUrl.INSTANCE.parse(value);
        if (uri == null) throw new ParsingException("Unsupported media.ccc.de conference URL: " + value);
        final List<String> path = MediaCCCUrl.INSTANCE.pathSegments(uri);
        final boolean apiHost = MediaCCCUrl.INSTANCE.hasHost(uri, "api.media.ccc.de")
                || MediaCCCUrl.INSTANCE.hasHost(uri, "media.ccc.de");
        if (apiHost && path.size() == 3 && "public".equals(path.get(0))
                && "conferences".equals(path.get(1))) return path.get(2);
        if (MediaCCCUrl.INSTANCE.hasHost(uri, "media.ccc.de") && path.size() == 2
                && ("b".equals(path.get(0)) || "c".equals(path.get(0)))) return path.get(1);
        throw new ParsingException("Unsupported media.ccc.de conference URL: " + value);
    }
    @Override public boolean onAcceptUrl(final String value) {
        try {
            getId(value);
            return true;
        } catch (final ParsingException ignored) {
            return false;
        }
    }
    @Override public String[] getAvailableContentFilter() { return new String[] { ChannelTabs.VIDEOS }; }
}
