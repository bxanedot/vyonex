// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

final class YoutubeRouteSupport {
    private YoutubeRouteSupport() { }

    static URL parse(final String value) {
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
                || YoutubeParsingHelper.isInvidiousURL(url));
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
}
