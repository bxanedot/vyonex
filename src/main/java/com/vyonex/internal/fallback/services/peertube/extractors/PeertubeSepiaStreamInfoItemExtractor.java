// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonObject;

import java.net.URI;

public final class PeertubeSepiaStreamInfoItemExtractor extends PeertubeStreamInfoItemExtractor {
    private final IllegalArgumentException embedUrlError;

    public PeertubeSepiaStreamInfoItemExtractor(final JsonObject item, final String baseUrl) {
        super(item, baseUrl);
        IllegalArgumentException error = null;
        if (embedUrl != null) {
            try {
                final URI location = URI.create(embedUrl);
                if (location.getScheme() != null && location.getRawAuthority() != null) {
                    final String origin = location.getScheme() + "://" + location.getRawAuthority();
                    final String embedPath = item.getString("embedPath");
                    final String path = location.getRawPath();
                    final int pathStart = embedPath == null || embedPath.isBlank()
                            || path == null ? -1 : path.indexOf(embedPath);
                    setBaseUrl(pathStart >= 0 ? origin + path.substring(0, pathStart) : origin);
                }
            } catch (final IllegalArgumentException parseError) {
                error = parseError;
            }
        }
        embedUrlError = error;
    }

    @Override public String getUrl() throws ParsingException {
        if (embedUrlError != null) {
            throw new ParsingException("Invalid Sepia video URL", embedUrlError);
        }
        return super.getUrl();
    }
}
