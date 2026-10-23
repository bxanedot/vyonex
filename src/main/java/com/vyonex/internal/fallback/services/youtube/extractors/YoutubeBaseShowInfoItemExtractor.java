// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getThumbnailsFromInfoItem;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getUrlFromNavigationEndpoint;

/** Shared VyonEx mapping for playlist-like show cards. */
abstract class YoutubeBaseShowInfoItemExtractor implements PlaylistInfoItemExtractor {
    @Nonnull
    protected final JsonObject showRenderer;

    YoutubeBaseShowInfoItemExtractor(@Nonnull final JsonObject showRenderer) {
        this.showRenderer = Objects.requireNonNull(showRenderer, "show renderer is required");
    }

    @Override
    public String getName() throws ParsingException {
        final String name = showRenderer.getString("title");
        if (name == null || name.isBlank()) {
            throw new ParsingException("Show title is missing");
        }
        return name;
    }

    @Override
    public String getUrl() throws ParsingException {
        final String url = getUrlFromNavigationEndpoint(showRenderer.getObject("navigationEndpoint"));
        if (url == null || url.isBlank()) {
            throw new ParsingException("Show URL is missing");
        }
        return url;
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        return getThumbnailsFromInfoItem(
                showRenderer.getObject("thumbnailRenderer").getObject("showCustomThumbnailRenderer"));
    }

    @Override
    public long getStreamCount() throws ParsingException {
        final String count = getTextFromObject(showRenderer.getObject("thumbnailOverlays")
                .getObject("thumbnailOverlayBottomPanelRenderer").getObject("text"));
        if (count == null || count.isBlank()) {
            throw new ParsingException("Show stream count is missing");
        }

        try {
            return Utils.mixedNumberWordToLong(count);
        } catch (final NumberFormatException | ParsingException error) {
            throw new ParsingException("Could not parse show stream count", error);
        }
    }
}
