// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class BandcampUrl {
    private BandcampUrl() { }

    static URI parse(final String value) throws ParsingException {
        try {
            final URI uri = URI.create(value);
            final String scheme = uri.getScheme();
            if (uri.getHost() != null && ("http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme))) return uri;
        } catch (IllegalArgumentException | NullPointerException ignored) { }
        throw new ParsingException("Invalid Bandcamp URL: " + value);
    }

    static String https(final String url) {
        return url != null && url.startsWith("http://") ? "https://" + url.substring(7) : url;
    }

    static boolean isArtistPage(final String url) throws ParsingException {
        final URI uri = parse(url);
        if ("daily.bandcamp.com".equalsIgnoreCase(uri.getHost())) return false;
        final List<String> path = segments(uri);
        if (path.size() > 1 || (path.size() == 1 && !List.of(
                "releases", "music", "album", "track").contains(path.get(0).toLowerCase(Locale.ROOT)))) {
            return false;
        }
        return BandcampExtractorHelper.isArtistDomain(url);
    }

    static boolean isArtistContent(final String url, final String kind) throws ParsingException {
        final URI uri = parse(url);
        final String path = uri.getPath().toLowerCase(Locale.ROOT);
        final String prefix = "/" + kind + "/";
        return path.startsWith(prefix) && path.length() > prefix.length()
                && BandcampExtractorHelper.isArtistDomain(url);
    }

    static List<String> segments(final URI uri) {
        return Arrays.stream(uri.getPath().split("/"))
                .filter(segment -> !segment.isEmpty())
                .toList();
    }
}
