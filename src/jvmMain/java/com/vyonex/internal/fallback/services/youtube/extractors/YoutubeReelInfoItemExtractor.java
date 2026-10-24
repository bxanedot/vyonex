// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray;

/** Maps legacy YouTube short-form cards into VyonEx stream items. */
public class YoutubeReelInfoItemExtractor implements StreamInfoItemExtractor {
    @Nullable private final String name;
    @Nullable private final String videoId;
    @Nullable private final String viewCountText;
    private final List<Image> thumbnails;

    public YoutubeReelInfoItemExtractor(@Nonnull final JsonObject item) {
        final JsonObject reel = Objects.requireNonNull(item, "reel item is required");
        name = getTextFromObject(reel.getObject("headline"));
        videoId = reel.getString("videoId");
        viewCountText = getTextFromObject(reel.getObject("viewCountText"));
        final JsonArray thumbnailItems = reel.getObject("thumbnail").getArray("thumbnails");
        thumbnails = getImagesFromThumbnailsArray(thumbnailItems);
    }

    @Override
    public String getName() throws ParsingException {
        if (name == null || name.isBlank()) {
            throw new ParsingException("Short-form title is missing");
        }
        return name;
    }

    @Override
    public String getUrl() throws ParsingException {
        if (videoId == null || videoId.isBlank()) {
            throw new ParsingException("Short-form video ID is missing");
        }
        try {
            return YoutubeStreamLinkHandlerFactory.getInstance().getUrl(videoId);
        } catch (final Exception error) {
            throw new ParsingException("Could not build short-form URL", error);
        }
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException { return thumbnails; }

    @Override
    public StreamType getStreamType() {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public long getViewCount() throws ParsingException {
        if (viewCountText == null || viewCountText.isBlank()) {
            throw new ParsingException("Short-form view count is missing");
        }
        if (viewCountText.toLowerCase(Locale.ROOT).contains("no views")) return 0;
        try {
            return Utils.mixedNumberWordToLong(viewCountText);
        } catch (final NumberFormatException | ParsingException error) {
            throw new ParsingException("Could not parse short-form view count", error);
        }
    }

    @Override
    public boolean isShortFormContent() {
        return true;
    }

    @Override public boolean isAd() { return false; }
    @Override public long getDuration() { return -1; }
    @Override public String getUploaderName() throws ParsingException { return null; }
    @Override public String getUploaderUrl() throws ParsingException { return null; }
    @Override public boolean isUploaderVerified() { return false; }
    @Nullable @Override public String getTextualUploadDate() { return null; }
    @Nullable @Override public DateWrapper getUploadDate() { return null; }
}
