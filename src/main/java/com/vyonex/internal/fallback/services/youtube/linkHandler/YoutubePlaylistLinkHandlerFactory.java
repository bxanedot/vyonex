// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;

import java.util.List;

public final class YoutubePlaylistLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final YoutubePlaylistLinkHandlerFactory INSTANCE =
            new YoutubePlaylistLinkHandlerFactory();
    private static final String ID_PATTERN = "[A-Za-z0-9_-]{10,}";

    private YoutubePlaylistLinkHandlerFactory() { }

    public static YoutubePlaylistLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getUrl(final String id, final List<String> filters, final String sort) {
        return "https://www.youtube.com/playlist?list=" + id;
    }

    @Override public String getId(final String value) throws ParsingException {
        final var url = YoutubeRouteSupport.parse(value);
        if (!YoutubeRouteSupport.isYoutubeOrMirror(url)
                || !("/watch".equals(url.getPath()) || "/playlist".equals(url.getPath()))) {
            throw new ParsingException("URL is not a YouTube playlist or video URL");
        }
        final String id = YoutubeRouteSupport.queryValue(url, "list");
        if (id == null || !id.matches(ID_PATTERN)) {
            throw new ParsingException("URL has no valid playlist ID");
        }
        return id;
    }

    @Override public boolean onAcceptUrl(final String url) {
        try {
            getId(url);
            return true;
        } catch (final ParsingException e) {
            return false;
        }
    }

    @Override public ListLinkHandler fromUrl(final String value) throws ParsingException {
        final var url = YoutubeRouteSupport.parse(value);
        final String playlistId = YoutubeRouteSupport.queryValue(url, "list");
        if (url != null && playlistId != null && YoutubeParsingHelper.isYoutubeMixId(playlistId)) {
            String videoId = YoutubeRouteSupport.queryValue(url, "v");
            if (videoId == null) videoId = YoutubeParsingHelper.extractVideoIdFromMixId(playlistId);
            if (videoId != null && !videoId.isBlank()) {
                final String canonical = "https://www.youtube.com/watch?v=" + videoId
                        + "&list=" + playlistId;
                return new ListLinkHandler(new LinkHandler(value, canonical, playlistId));
            }
        }
        return super.fromUrl(value);
    }
}
