// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonObject;

import java.net.URI;

public final class PeertubeSepiaStreamInfoItemExtractor extends PeertubeStreamInfoItemExtractor {
    public PeertubeSepiaStreamInfoItemExtractor(final JsonObject item, final String baseUrl) {
        super(item, baseUrl);
    }

    @Override public String getUrl() throws ParsingException {
        final String embedUrl = item.getString("embedUrl");
        if (embedUrl != null) {
            try {
                final URI uri = URI.create(embedUrl);
                if (uri.getScheme() != null && uri.getRawAuthority() != null) {
                    setBaseUrl(uri.getScheme() + "://" + uri.getRawAuthority());
                }
            } catch (final IllegalArgumentException e) {
                throw new ParsingException("Invalid Sepia video URL", e);
            }
        }
        return super.getUrl();
    }
}
