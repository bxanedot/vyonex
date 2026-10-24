// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.regex.Pattern;

abstract class YoutubeBaseShowInfoItemExtractor implements PlaylistInfoItemExtractor {
    private static final Pattern PLAYLIST_ID = Pattern.compile("[A-Za-z0-9_-]+");
    protected final JsonObject showRenderer;

    YoutubeBaseShowInfoItemExtractor(final JsonObject showRenderer) {
        if (showRenderer == null) throw new IllegalArgumentException("Show data is required");
        this.showRenderer = showRenderer;
    }

    @Override
    public String getName() throws ParsingException {
        final String title = showRenderer.getString("title");
        if (title == null || title.isBlank()) throw new ParsingException("Show title is missing");
        return title;
    }

    @Override
    public String getUrl() throws ParsingException {
        final String browseId = showRenderer.getObject("navigationEndpoint")
                .getObject("browseEndpoint").getString("browseId");
        if (browseId == null || !browseId.startsWith("VL")) {
            throw new ParsingException("Show playlist id is missing or invalid");
        }
        final String playlistId = browseId.substring(2);
        if (!PLAYLIST_ID.matcher(playlistId).matches()) {
            throw new ParsingException("Show playlist id is missing or invalid");
        }
        return "https://www.youtube.com/playlist?list=" + playlistId;
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        final JsonArray thumbnails = showRenderer.getObject("thumbnailRenderer")
                .getObject("showCustomThumbnailRenderer")
                .getObject("thumbnail").getArray("thumbnails");
        return YoutubeParsingHelper.getImagesFromThumbnailsArray(thumbnails);
    }

    @Override
    public long getStreamCount() throws ParsingException {
        final String count = YoutubeParsingHelper.getTextFromObject(showRenderer
                .getObject("thumbnailOverlays")
                .getObject("thumbnailOverlayBottomPanelRenderer").getObject("text"));
        if (count == null || count.isBlank()) throw new ParsingException("Show video count is missing");
        try {
            return Utils.mixedNumberWordToLong(count);
        } catch (NumberFormatException error) {
            throw new ParsingException("Show video count is invalid", error);
        }
    }
}
