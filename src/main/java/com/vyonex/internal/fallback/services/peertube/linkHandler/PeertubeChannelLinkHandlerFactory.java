// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
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

    @Override
    public String getId(final String url) throws ParsingException {
        final List<String> path = PeertubeUrl.segments(url);
        final int index = path.size() > 2 && "api".equals(path.get(0))
                && "v1".equals(path.get(1)) ? 2 : 0;
        if (path.size() <= index + 1) throw new ParsingException("URL has no channel ID");
        final String prefix = switch (path.get(index)) {
            case "accounts", "a" -> "accounts";
            case "video-channels", "c" -> "video-channels";
            default -> throw new ParsingException("URL does not identify a PeerTube channel");
        };
        return prefix + "/" + path.get(index + 1);
    }

    @Override
    public String getUrl(final String id, final List<String> contentFilters,
                         final String sortFilter) throws ParsingException {
        return getUrl(id, contentFilters, sortFilter, ServiceList.PeerTube.getBaseUrl());
    }

    @Override
    public String getUrl(final String id, final List<String> contentFilters,
                         final String sortFilter, final String baseUrl) throws ParsingException {
        return baseUrl + "/" + canonicalId(id);
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

    private String canonicalId(final String id) throws ParsingException {
        final String[] parts = id.split("/", -1);
        if (parts.length == 2 && !parts[1].isBlank()) {
            return switch (parts[0]) {
                case "accounts", "a" -> "accounts/" + parts[1];
                case "video-channels", "c" -> "video-channels/" + parts[1];
                default -> throw new ParsingException("Unsupported PeerTube channel ID: " + id);
            };
        }
        if (parts.length == 1 && !id.isBlank()) return "accounts/" + id;
        throw new ParsingException("Invalid PeerTube channel ID: " + id);
    }
}
