// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.utils.Parser;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getUrlFromNavigationEndpoint;

/** Converts YouTube Music artist search results into channel items. */
public class YoutubeMusicArtistInfoItemExtractor implements ChannelInfoItemExtractor {
    private final JsonObject artist;

    public YoutubeMusicArtistInfoItemExtractor(final JsonObject artist) {
        this.artist = Objects.requireNonNull(artist, "artist result is required");
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        try {
            return getImagesFromThumbnailsArray(artist.getObject("thumbnail")
                    .getObject("musicThumbnailRenderer").getObject("thumbnail")
                    .getArray("thumbnails"));
        } catch (final RuntimeException error) {
            throw new ParsingException("Could not read artist thumbnails", error);
        }
    }

    @Override
    public String getName() throws ParsingException {
        final JsonArray columns = artist.getArray("flexColumns");
        final String name = columns.isEmpty() ? null : getTextFromObject(columns.getObject(0)
                .getObject("musicResponsiveListItemFlexColumnRenderer").getObject("text"));
        if (name == null || name.isBlank()) {
            throw new ParsingException("Artist name is missing");
        }
        return name;
    }

    @Override
    public String getUrl() throws ParsingException {
        final String url = getUrlFromNavigationEndpoint(artist.getObject("navigationEndpoint"));
        if (url == null || url.isBlank()) {
            throw new ParsingException("Artist URL is missing");
        }
        return url;
    }

    @Override
    public long getSubscriberCount() throws ParsingException {
        final JsonArray columns = artist.getArray("flexColumns");
        if (columns.isEmpty()) throw new ParsingException("Artist audience size is missing");

        final JsonArray runs = columns.getObject(columns.size() - 1)
                .getObject("musicResponsiveListItemFlexColumnRenderer")
                .getObject("text").getArray("runs");
        if (runs.isEmpty()) throw new ParsingException("Artist audience size is missing");

        final String label = runs.getObject(runs.size() - 1).getString("text");
        if (label == null || label.isBlank()) {
            throw new ParsingException("Artist audience size is missing");
        }
        try {
            return Utils.mixedNumberWordToLong(label);
        } catch (final Parser.RegexException ignored) {
            return 0;
        } catch (final NumberFormatException error) {
            throw new ParsingException("Could not parse artist audience size", error);
        }
    }

    @Override public long getStreamCount() { return ListExtractor.ITEM_COUNT_UNKNOWN; }
    @Override public boolean isVerified() { return true; }
    @Nullable @Override public String getDescription() { return null; }
}
