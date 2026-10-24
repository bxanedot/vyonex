// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper;

import java.util.List;

public final class MediaCCCStreamLinkHandlerFactory extends LinkHandlerFactory {
    public static final String VIDEO_API_ENDPOINT = "https://api.media.ccc.de/public/events/";
    private static final String EVENT_PAGE = "https://media.ccc.de/v/";
    private static final String LIVE_PAGE = "https://streaming.media.ccc.de/";
    private static final MediaCCCStreamLinkHandlerFactory INSTANCE =
            new MediaCCCStreamLinkHandlerFactory();

    private MediaCCCStreamLinkHandlerFactory() { }
    public static MediaCCCStreamLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getId(final String value) throws ParsingException {
        final MediaCCCUrl.Parsed uri = MediaCCCUrl.INSTANCE.parse(value);
        if (uri == null) throw new ParsingException("Unsupported media.ccc.de stream URL: " + value);
        final List<String> path = MediaCCCUrl.INSTANCE.pathSegments(uri);
        if (MediaCCCUrl.INSTANCE.hasHost(uri, "streaming.media.ccc.de") && path.size() == 2) {
            final String id = path.get(0) + "/" + path.get(1);
            if (MediaCCCParsingHelper.isLiveStreamId(id)) return id;
        }
        if (MediaCCCUrl.INSTANCE.hasHost(uri, "media.ccc.de") && path.size() == 2
                && "v".equals(path.get(0))) return path.get(1);
        final boolean eventApiHost = MediaCCCUrl.INSTANCE.hasHost(uri, "api.media.ccc.de")
                || MediaCCCUrl.INSTANCE.hasHost(uri, "media.ccc.de");
        if (eventApiHost && path.size() == 3 && "public".equals(path.get(0))
                && "events".equals(path.get(1))) return path.get(2);
        throw new ParsingException("Unsupported media.ccc.de stream URL: " + value);
    }

    @Override public String getUrl(final String id) {
        return (MediaCCCParsingHelper.isLiveStreamId(id) ? LIVE_PAGE : EVENT_PAGE) + id;
    }
    @Override public boolean onAcceptUrl(final String value) {
        try {
            getId(value);
            return true;
        } catch (final ParsingException ignored) {
            return false;
        }
    }
}
