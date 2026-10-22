// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.linkHandler;

import com.vyonex.internal.fallback.ServiceList;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;

public final class PeertubeChannelLinkHandlerFactory extends ListLinkHandlerFactory {
    public static final String API_ENDPOINT = "/api/v1/";
    private static final PeertubeChannelLinkHandlerFactory INSTANCE =
            new PeertubeChannelLinkHandlerFactory();

    private PeertubeChannelLinkHandlerFactory() { }
    public static PeertubeChannelLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getId(final String url) throws ParsingException {
        final List<String> path = PeertubeUrl.segments(url);
        final boolean apiRoute = path.size() > 2 && "api".equals(path.get(0))
                && "v1".equals(path.get(1));
        final int start = apiRoute ? 2 : 0;
        if (path.size() <= start + 1) throw new ParsingException("URL has no channel ID");
        final String type = switch (path.get(start)) {
            case "accounts", "a" -> "accounts";
            case "video-channels", "c" -> "video-channels";
            default -> throw new ParsingException("URL does not identify a PeerTube channel");
        };
        return type + "/" + path.get(start + 1);
    }

    @Override public String getUrl(final String id, final List<String> filters,
                                   final String sort) throws ParsingException {
        return getUrl(id, filters, sort, ServiceList.PeerTube.getBaseUrl());
    }
    @Override public String getUrl(final String id, final List<String> filters,
                                   final String sort, final String baseUrl)
            throws ParsingException {
        return baseUrl + "/" + canonicalId(id);
    }
    @Override public boolean onAcceptUrl(final String url) {
        try {
            getId(url);
            return true;
        } catch (final ParsingException ignored) {
            return false;
        }
    }

    private static String canonicalId(final String id) throws ParsingException {
        final String[] segments = id.split("/", -1);
        if (segments.length == 1 && !id.isBlank()) return "accounts/" + id;
        if (segments.length == 2 && !segments[1].isBlank()) {
            return switch (segments[0]) {
                case "accounts", "a" -> "accounts/" + segments[1];
                case "video-channels", "c" -> "video-channels/" + segments[1];
                default -> throw new ParsingException("Unsupported PeerTube channel ID: " + id);
            };
        }
        throw new ParsingException("Invalid PeerTube channel ID: " + id);
    }
}
