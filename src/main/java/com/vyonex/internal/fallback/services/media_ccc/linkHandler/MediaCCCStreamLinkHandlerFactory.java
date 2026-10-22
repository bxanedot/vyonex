// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper;

import java.net.URI;
import java.util.List;

public final class MediaCCCStreamLinkHandlerFactory extends LinkHandlerFactory {
    private static final MediaCCCStreamLinkHandlerFactory INSTANCE =
            new MediaCCCStreamLinkHandlerFactory();

    public static final String VIDEO_API_ENDPOINT = "https://api.media.ccc.de/public/events/";
    private static final String VIDEO_PATH = "https://media.ccc.de/v/";
    private static final String LIVE_STREAM_PATH = "https://streaming.media.ccc.de/";

    private MediaCCCStreamLinkHandlerFactory() { }

    public static MediaCCCStreamLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getId(final String url) throws ParsingException {
        final URI uri = MediaCCCUrl.parse(url);
        final List<String> path = MediaCCCUrl.pathSegments(uri);
        if (MediaCCCUrl.hasHost(uri, "streaming.media.ccc.de") && path.size() == 2) {
            final String id = path.get(0) + "/" + path.get(1);
            if (MediaCCCParsingHelper.isLiveStreamId(id)) return id;
        }
        if (MediaCCCUrl.hasHost(uri, "media.ccc.de") && path.size() == 2
                && "v".equals(path.get(0))) return path.get(1);
        if ((MediaCCCUrl.hasHost(uri, "api.media.ccc.de")
                || MediaCCCUrl.hasHost(uri, "media.ccc.de")) && path.size() == 3
                && "public".equals(path.get(0)) && "events".equals(path.get(1))) {
            return path.get(2);
        }
        throw new ParsingException("Unsupported media.ccc.de stream URL: " + url);
    }

    @Override
    public String getUrl(final String id) {
        return MediaCCCParsingHelper.isLiveStreamId(id)
                ? LIVE_STREAM_PATH + id : VIDEO_PATH + id;
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
}
