// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonObject;

import java.net.URI;

public final class PeertubeSepiaStreamInfoItemExtractor extends PeertubeStreamInfoItemExtractor {
    public PeertubeSepiaStreamInfoItemExtractor(final JsonObject item, final String baseUrl) {
        super(item, baseUrl);
    }

    @Override public String getUrl() throws ParsingException {
        final String embed = item.getString("embedUrl");
        if (embed != null) {
            try {
                final URI location = URI.create(embed);
                if (location.getScheme() != null && location.getRawAuthority() != null) {
                    setBaseUrl(location.getScheme() + "://" + location.getRawAuthority());
                }
            } catch (final IllegalArgumentException error) {
                throw new ParsingException("Invalid Sepia video URL", error);
            }
        }
        return super.getUrl();
    }
}
