// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler;

import com.vyonex.internal.fallback.ServiceList;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory;

import java.util.List;

public final class PeertubeStreamLinkHandlerFactory extends LinkHandlerFactory {
    public static final String VIDEO_API_ENDPOINT = "/api/v1/videos/";
    private static final PeertubeStreamLinkHandlerFactory INSTANCE =
            new PeertubeStreamLinkHandlerFactory();

    private PeertubeStreamLinkHandlerFactory() { }

    public static PeertubeStreamLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getUrl(final String id) {
        return getUrl(id, ServiceList.PeerTube.getBaseUrl());
    }

    @Override
    public String getUrl(final String id, final String baseUrl) {
        return baseUrl + "/videos/watch/" + id;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        final List<String> path = PeertubeUrl.segments(url);
        if (path.size() == 2 && "w".equals(path.get(0))) return path.get(1);
        if (path.size() == 2 && "videos".equals(path.get(0))) return path.get(1);
        if (path.size() == 3 && "videos".equals(path.get(0))
                && ("watch".equals(path.get(1)) || "embed".equals(path.get(1)))) {
            return path.get(2);
        }
        throw new ParsingException("URL does not identify a PeerTube video");
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        try {
            getId(url);
            return true;
        } catch (final ParsingException e) {
            return false;
        }
    }
}
