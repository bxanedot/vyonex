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
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getThumbnailsFromInfoItem;

/** Maps legacy YouTube short-form cards into VyonEx stream items. */
public class YoutubeReelInfoItemExtractor implements StreamInfoItemExtractor {
    private final JsonObject item;

    public YoutubeReelInfoItemExtractor(@Nonnull final JsonObject item) {
        this.item = Objects.requireNonNull(item, "reel item is required");
    }

    @Override
    public String getName() throws ParsingException {
        final String name = getTextFromObject(item.getObject("headline"));
        if (name == null || name.isBlank()) {
            throw new ParsingException("Short-form title is missing");
        }
        return name;
    }

    @Override
    public String getUrl() throws ParsingException {
        final String videoId = item.getString("videoId");
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
    public List<Image> getThumbnails() throws ParsingException {
        return getThumbnailsFromInfoItem(item);
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public long getViewCount() throws ParsingException {
        final String text = getTextFromObject(item.getObject("viewCountText"));
        if (text == null || text.isBlank()) {
            throw new ParsingException("Short-form view count is missing");
        }
        if (text.toLowerCase(Locale.ROOT).contains("no views")) return 0;
        try {
            return Utils.mixedNumberWordToLong(text);
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
