// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

final class MediaCCCUrl {
    private MediaCCCUrl() { }

    static URI parse(final String value) throws ParsingException {
        if (value == null || value.isBlank()) throw new ParsingException("URL is empty");
        final boolean hasScheme = value.regionMatches(true, 0, "http://", 0, 7)
                || value.regionMatches(true, 0, "https://", 0, 8);
        final String candidate = hasScheme ? value : "https://" + value;
        try {
            final URI uri = URI.create(candidate);
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new ParsingException("Unsupported URL: " + value);
            }
            return uri;
        } catch (final IllegalArgumentException error) {
            throw new ParsingException("Invalid URL: " + value, error);
        }
    }

    static List<String> pathSegments(final URI uri) {
        return Arrays.stream(uri.getPath().split("/"))
                .filter(part -> !part.isEmpty()).toList();
    }

    static boolean hasHost(final URI uri, final String host) {
        return host.equalsIgnoreCase(uri.getHost());
    }
}
