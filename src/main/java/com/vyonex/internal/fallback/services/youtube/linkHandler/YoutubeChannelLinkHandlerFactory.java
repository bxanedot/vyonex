// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class YoutubeChannelLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final YoutubeChannelLinkHandlerFactory INSTANCE =
            new YoutubeChannelLinkHandlerFactory();
    private static final Set<String> RESERVED_PATHS = Set.of("watch", "playlist", "feed",
            "results", "search", "shorts", "live", "embed", "gaming", "podcasts",
            "account", "reporthistory", "redirect", "attribution_link", "channel", "user", "c");

    private YoutubeChannelLinkHandlerFactory() { }

    public static YoutubeChannelLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getUrl(final String id, final List<String> filters, final String sort) {
        return "https://www.youtube.com/" + id;
    }

    @Override public String getId(final String value) throws ParsingException {
        final var url = YoutubeRouteSupport.parse(value);
        if (!YoutubeRouteSupport.isYoutubeOrMirror(url)) {
            throw new ParsingException("URL is not a supported video channel");
        }
        final String[] parts = url.getPath().split("/");
        final List<String> segments = java.util.Arrays.stream(parts)
                .filter(part -> !part.isEmpty()).toList();
        if (segments.isEmpty()) throw new ParsingException("Channel URL has no path");
        final String first = segments.get(0);
        if (first.startsWith("@")) return first;
        if (segments.size() >= 2 && Set.of("channel", "user", "c")
                .contains(first.toLowerCase(Locale.ROOT))) {
            return first + "/" + segments.get(1);
        }
        if (segments.size() == 1 && !RESERVED_PATHS.contains(first.toLowerCase(Locale.ROOT))) {
            return first;
        }
        throw new ParsingException("URL is not a channel, user, or handle route");
    }

    @Override public boolean onAcceptUrl(final String url) {
        try {
            getId(url);
            return true;
        } catch (final ParsingException e) {
            return false;
        }
    }
}
