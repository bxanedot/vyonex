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

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray;

/** Maps a YouTube Shorts card into VyonEx's stream item API. */
class YoutubeShortsLockupInfoItemExtractor implements StreamInfoItemExtractor {
    private final JsonObject card;

    YoutubeShortsLockupInfoItemExtractor(@Nonnull final JsonObject card) {
        this.card = Objects.requireNonNull(card, "Shorts card is required");
    }

    @Override
    public String getName() throws ParsingException {
        final String title = card.getObject("overlayMetadata")
                .getObject("primaryText").getString("content");
        if (title == null || title.isBlank()) {
            throw new ParsingException("Shorts title is missing");
        }
        return title;
    }

    @Override
    public String getUrl() throws ParsingException {
        String videoId = card.getObject("onTap").getObject("innertubeCommand")
                .getObject("reelWatchEndpoint").getString("videoId");
        if (videoId == null || videoId.isBlank()) {
            videoId = card.getObject("inlinePlayerData").getObject("onVisible")
                    .getObject("innertubeCommand").getObject("watchEndpoint")
                    .getString("videoId");
        }
        if (videoId == null || videoId.isBlank()) {
            throw new ParsingException("Shorts video ID is missing");
        }
        try {
            return YoutubeStreamLinkHandlerFactory.getInstance().getUrl(videoId);
        } catch (final Exception error) {
            throw new ParsingException("Could not build Shorts URL", error);
        }
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        final JsonObject thumbnail;
        if (card.has("thumbnail")) {
            thumbnail = card.getObject("thumbnail");
        } else {
            thumbnail = card.getObject("thumbnailViewModel")
                    .getObject("thumbnailViewModel").getObject("image");
        }
        return getImagesFromThumbnailsArray(thumbnail.getArray("sources"));
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public long getViewCount() throws ParsingException {
        final String label = card.getObject("overlayMetadata")
                .getObject("secondaryText").getString("content");
        if (label == null || label.isBlank()) {
            throw new ParsingException("Shorts view count is missing");
        }
        if (label.contains("✪")) return -1;
        if (label.toLowerCase(Locale.ROOT).contains("no views")) return 0;
        try {
            return Utils.mixedNumberWordToLong(label);
        } catch (final NumberFormatException | ParsingException error) {
            throw new ParsingException("Could not parse Shorts view count", error);
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
