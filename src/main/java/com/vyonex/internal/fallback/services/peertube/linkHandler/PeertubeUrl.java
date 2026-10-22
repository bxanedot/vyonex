// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

final class PeertubeUrl {
    private PeertubeUrl() { }

    static URI parse(final String value) throws ParsingException {
        try {
            final URI uri = new URI(value);
            if (uri.getHost() != null && uri.getUserInfo() == null
                    && ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) return uri;
        } catch (final URISyntaxException | NullPointerException ignored) { }
        throw new ParsingException("Invalid PeerTube URL: " + value);
    }

    static List<String> segments(final String value) throws ParsingException {
        final String path = parse(value).getRawPath();
        return path == null ? List.of() : Arrays.stream(path.split("/"))
                .filter(segment -> !segment.isEmpty()).toList();
    }

    static boolean accepts(final String value) {
        try {
            parse(value);
            return true;
        } catch (final ParsingException e) {
            return false;
        }
    }
}
