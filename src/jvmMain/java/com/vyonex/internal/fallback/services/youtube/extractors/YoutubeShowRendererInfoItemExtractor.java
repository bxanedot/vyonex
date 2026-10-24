// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getUrlFromObject;

/** Adds uploader details to the shared VyonEx show-card mapping. */
class YoutubeShowRendererInfoItemExtractor extends YoutubeBaseShowInfoItemExtractor {
    private final JsonObject shortByline;
    private final JsonObject longByline;

    YoutubeShowRendererInfoItemExtractor(@Nonnull final JsonObject renderer) {
        super(renderer);
        this.shortByline = renderer.getObject("shortBylineText");
        this.longByline = renderer.getObject("longBylineText");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        final String name = getTextFromObject(longByline);
        if (name != null && !name.isBlank()) return name;
        final String fallback = getTextFromObject(shortByline);
        if (fallback == null || fallback.isBlank()) {
            throw new ParsingException("Show uploader name is missing");
        }
        return fallback;
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        final String url = getUrlFromObject(longByline);
        if (url != null && !url.isBlank()) return url;
        final String fallback = getUrlFromObject(shortByline);
        if (fallback == null || fallback.isBlank()) {
            throw new ParsingException("Show uploader URL is missing");
        }
        return fallback;
    }

    @Override
    public boolean isUploaderVerified() {
        return false;
    }
}
