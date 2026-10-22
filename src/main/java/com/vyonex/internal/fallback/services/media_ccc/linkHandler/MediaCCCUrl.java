// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

final class MediaCCCUrl {
    private MediaCCCUrl() { }

    static URI parse(final String value) throws ParsingException {
        if (value == null || value.isBlank()) throw new ParsingException("URL is empty");
        final String absolute = value.regionMatches(true, 0, "http://", 0, 7)
                || value.regionMatches(true, 0, "https://", 0, 8)
                ? value : "https://" + value;
        try {
            final URI uri = URI.create(absolute);
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new ParsingException("Unsupported URL: " + value);
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new ParsingException("Invalid URL: " + value, e);
        }
    }

    static List<String> pathSegments(final URI uri) {
        return Arrays.stream(uri.getPath().split("/"))
                .filter(segment -> !segment.isEmpty())
                .toList();
    }

    static boolean hasHost(final URI uri, final String host) {
        return host.equalsIgnoreCase(uri.getHost());
    }
}
