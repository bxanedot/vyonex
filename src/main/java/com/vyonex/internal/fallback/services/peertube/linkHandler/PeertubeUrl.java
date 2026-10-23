// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
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
            final URI parsed = new URI(value);
            final String scheme = parsed.getScheme();
            if (parsed.getHost() != null && parsed.getUserInfo() == null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return parsed;
            }
        } catch (final URISyntaxException | NullPointerException ignored) { }
        throw new ParsingException("Invalid PeerTube URL: " + value);
    }

    static List<String> segments(final String value) throws ParsingException {
        final String path = parse(value).getRawPath();
        if (path == null) return List.of();
        return Arrays.stream(path.split("/")).filter(part -> !part.isEmpty()).toList();
    }

    static boolean accepts(final String value) {
        try {
            parse(value);
            return true;
        } catch (final ParsingException ignored) {
            return false;
        }
    }
}
