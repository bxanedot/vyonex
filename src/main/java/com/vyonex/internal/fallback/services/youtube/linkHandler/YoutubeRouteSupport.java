// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class YoutubeRouteSupport {
    private YoutubeRouteSupport() { }

    static URL parse(final String value) {
        if (value == null || value.isBlank()) return null;
        try {
            final URI uri = new URI(value);
            final String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http")
                    || scheme.equalsIgnoreCase("https")) || uri.getHost() == null) return null;
            return uri.toURL();
        } catch (final URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return null;
        }
    }

    static boolean isYoutubeOrMirror(final URL url) {
        return url != null && (YoutubeParsingHelper.isYoutubeURL(url)
                || YoutubeParsingHelper.isYoutubeServiceURL(url)
                || YoutubeParsingHelper.isHooktubeURL(url)
                || YoutubeParsingHelper.isInvidiousURL(url)
                || YoutubeParsingHelper.isY2ubeURL(url));
    }

    static boolean hasPath(final URL url, final String path) {
        return isYoutubeOrMirror(url) && path.equals(url.getPath());
    }

    static boolean isChartsPath(final URL url, final String path) {
        return isChartsHost(url) && path.equals(url.getPath());
    }

    static boolean isChartsHost(final URL url) {
        return url != null && "charts.youtube.com".equals(url.getHost().toLowerCase(Locale.ROOT));
    }

    static String queryValue(final URL url, final String name) {
        if (url == null || url.getQuery() == null) return null;
        for (final String part : url.getQuery().split("&")) {
            final int separator = part.indexOf('=');
            final String key = separator < 0 ? part : part.substring(0, separator);
            if (URLDecoder.decode(key, StandardCharsets.UTF_8).equals(name)) {
                return URLDecoder.decode(separator < 0 ? "" : part.substring(separator + 1),
                        StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
