// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.utils.Parser;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getUrlFromNavigationEndpoint;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS;

/** Converts YouTube Music song and video search rows into stream items. */
public class YoutubeMusicSongOrVideoInfoItemExtractor implements StreamInfoItemExtractor {
    private final JsonObject item;
    private final JsonArray description;
    private final String searchType;

    public YoutubeMusicSongOrVideoInfoItemExtractor(final JsonObject item,
                                                    final JsonArray description,
                                                    final String searchType) {
        this.item = Objects.requireNonNull(item, "music row is required");
        this.description = Objects.requireNonNull(description, "description is required");
        this.searchType = Objects.requireNonNull(searchType, "search type is required");
    }

    @Override
    public String getUrl() throws ParsingException {
        final String videoId = item.getObject("playlistItemData").getString("videoId");
        if (videoId == null || videoId.isBlank()) throw new ParsingException("Video ID is missing");
        return "https://music.youtube.com/watch?v=" + videoId;
    }

    @Override
    public String getName() throws ParsingException {
        final JsonArray columns = item.getArray("flexColumns");
        final String title = columns.isEmpty() ? null : getTextFromObject(columns.getObject(0)
                .getObject("musicResponsiveListItemFlexColumnRenderer").getObject("text"));
        if (title == null || title.isBlank()) throw new ParsingException("Music title is missing");
        return title;
    }

    @Override public StreamType getStreamType() { return StreamType.VIDEO_STREAM; }
    @Override public boolean isAd() { return false; }

    @Override
    public long getDuration() throws ParsingException {
        final String label = description.isEmpty() ? null
                : description.getObject(description.size() - 1).getString("text");
        if (label == null || label.isBlank()) throw new ParsingException("Duration is missing");
        try {
            return YoutubeParsingHelper.parseDurationString(label);
        } catch (final NumberFormatException error) {
            throw new ParsingException("Could not parse music duration", error);
        }
    }

    @Override
    public String getUploaderName() throws ParsingException {
        final String name = description.isEmpty() ? null : description.getObject(0).getString("text");
        if (name == null || name.isBlank()) throw new ParsingException("Uploader name is missing");
        return name;
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        if (MUSIC_VIDEOS.equals(searchType)) {
            for (final JsonObject menuItem : item.getObject("menu").getObject("menuRenderer")
                    .getArray("items").streamAsJsonObjects().toList()) {
                final JsonObject artist = menuItem.getObject("menuNavigationItemRenderer");
                if ("ARTIST".equals(artist.getObject("icon").getString("iconType"))) {
                    return getUrlFromNavigationEndpoint(artist.getObject("navigationEndpoint"));
                }
            }
            return null;
        }

        final JsonArray columns = item.getArray("flexColumns");
        if (columns.size() < 2) throw new ParsingException("Uploader details are missing");
        final JsonArray runs = columns.getObject(1)
                .getObject("musicResponsiveListItemFlexColumnRenderer")
                .getObject("text").getArray("runs");
        if (runs.isEmpty()) throw new ParsingException("Uploader details are missing");
        final JsonObject firstRun = runs.getObject(0);
        if (!firstRun.has("navigationEndpoint")) return null;

        final String url = getUrlFromNavigationEndpoint(firstRun.getObject("navigationEndpoint"));
        if (url == null || url.isBlank()) throw new ParsingException("Uploader URL is invalid");
        return url;
    }

    @Override public boolean isUploaderVerified() { return false; }
    @Override public String getTextualUploadDate() { return null; }
    @Override public DateWrapper getUploadDate() { return null; }

    @Override
    public long getViewCount() throws ParsingException {
        if (MUSIC_SONGS.equals(searchType)) return -1;
        if (description.size() < 3) throw new ParsingException("View count is missing");
        final String label = description.getObject(description.size() - 3).getString("text");
        if (label == null || label.isBlank()) throw new ParsingException("View count is missing");
        try {
            return Utils.mixedNumberWordToLong(label);
        } catch (final Parser.RegexException ignored) {
            return 0;
        } catch (final NumberFormatException error) {
            throw new ParsingException("Could not parse music view count", error);
        }
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        try {
            return getImagesFromThumbnailsArray(item.getObject("thumbnail")
                    .getObject("musicThumbnailRenderer").getObject("thumbnail")
                    .getArray("thumbnails"));
        } catch (final RuntimeException error) {
            throw new ParsingException("Could not read music item thumbnails", error);
        }
    }
}
