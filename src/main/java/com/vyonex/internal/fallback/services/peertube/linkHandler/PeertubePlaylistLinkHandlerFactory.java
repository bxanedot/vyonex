// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler;

import com.vyonex.internal.fallback.ServiceList;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;

public final class PeertubePlaylistLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final PeertubePlaylistLinkHandlerFactory INSTANCE =
            new PeertubePlaylistLinkHandlerFactory();

    private PeertubePlaylistLinkHandlerFactory() { }

    public static PeertubePlaylistLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getUrl(final String id, final List<String> contentFilters,
                         final String sortFilter) {
        return getUrl(id, contentFilters, sortFilter, ServiceList.PeerTube.getBaseUrl());
    }

    @Override
    public String getUrl(final String id, final List<String> contentFilters,
                         final String sortFilter, final String baseUrl) {
        return baseUrl + "/api/v1/video-playlists/" + id;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        final List<String> path = PeertubeUrl.segments(url);
        if (path.size() == 3 && "w".equals(path.get(0)) && "p".equals(path.get(1))) {
            return path.get(2);
        }
        if (path.size() == 4 && "videos".equals(path.get(0))
                && "watch".equals(path.get(1)) && "playlist".equals(path.get(2))) {
            return path.get(3);
        }
        if (path.size() == 4 && "api".equals(path.get(0)) && "v1".equals(path.get(1))
                && "video-playlists".equals(path.get(2))) {
            return path.get(3);
        }
        throw new ParsingException("URL does not identify a PeerTube playlist");
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
