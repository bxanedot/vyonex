// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler;

import com.vyonex.internal.fallback.ServiceList;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PeertubeTrendingLinkHandlerFactory extends ListLinkHandlerFactory {
    public static final String KIOSK_TRENDING = "Trending";
    public static final String KIOSK_MOST_LIKED = "Most liked";
    public static final String KIOSK_RECENT = "Recently added";
    public static final String KIOSK_LOCAL = "Local";
    public static final Map<String, String> KIOSK_MAP = Map.of(
            KIOSK_TRENDING, "%s/api/v1/videos?sort=-trending",
            KIOSK_MOST_LIKED, "%s/api/v1/videos?sort=-likes",
            KIOSK_RECENT, "%s/api/v1/videos?sort=-publishedAt",
            KIOSK_LOCAL, "%s/api/v1/videos?sort=-publishedAt&isLocal=true");
    private static final PeertubeTrendingLinkHandlerFactory INSTANCE =
            new PeertubeTrendingLinkHandlerFactory();

    private PeertubeTrendingLinkHandlerFactory() { }

    public static PeertubeTrendingLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getUrl(final String id, final List<String> filters, final String sortFilter)
            throws ParsingException {
        return getUrl(id, filters, sortFilter, ServiceList.PeerTube.getBaseUrl());
    }

    @Override
    public String getUrl(final String id, final List<String> filters, final String sortFilter,
                         final String baseUrl) throws ParsingException {
        final String template = KIOSK_MAP.get(id);
        if (template == null) throw new ParsingException("Unknown PeerTube kiosk: " + id);
        return String.format(template, baseUrl);
    }

    @Override
    public String getId(final String url) throws ParsingException {
        final URI uri = PeertubeUrl.parse(url);
        final String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        if (path.endsWith("/videos/trending")) return KIOSK_TRENDING;
        if (path.endsWith("/videos/most-liked")) return KIOSK_MOST_LIKED;
        if (path.endsWith("/videos/recently-added")) return KIOSK_RECENT;
        if (path.endsWith("/videos/local")) return KIOSK_LOCAL;
        if (!path.endsWith("/api/v1/videos")) {
            throw new ParsingException("URL does not identify a PeerTube kiosk");
        }
        final Map<String, String> query = query(uri.getRawQuery());
        if (query.equals(Map.of("sort", "-trending"))) return KIOSK_TRENDING;
        if (query.equals(Map.of("sort", "-likes"))) return KIOSK_MOST_LIKED;
        if (query.equals(Map.of("sort", "-publishedAt"))) return KIOSK_RECENT;
        if (query.equals(Map.of("sort", "-publishedAt", "isLocal", "true"))) {
            return KIOSK_LOCAL;
        }
        throw new ParsingException("URL does not identify a PeerTube kiosk");
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

    private static Map<String, String> query(final String query) throws ParsingException {
        final Map<String, String> values = new HashMap<>();
        if (query == null || query.isEmpty()) return values;
        try {
            for (final String entry : query.split("&")) {
                final int separator = entry.indexOf('=');
                final String key = separator < 0 ? entry : entry.substring(0, separator);
                final String value = separator < 0 ? "" : entry.substring(separator + 1);
                values.put(URLDecoder.decode(key, StandardCharsets.UTF_8),
                        URLDecoder.decode(value, StandardCharsets.UTF_8));
            }
        } catch (final IllegalArgumentException e) {
            throw new ParsingException("Invalid PeerTube kiosk query", e);
        }
        return values;
    }
}
