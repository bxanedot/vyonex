// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getUrlFromObject;

/** VyonEx parser for playlist results returned by YouTube search and channel tabs. */
public class YoutubePlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final JsonObject item;

    public YoutubePlaylistInfoItemExtractor(final JsonObject item) {
        this.item = Objects.requireNonNull(item, "playlist item is required");
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        final JsonArray groups = item.getArray("thumbnails");
        final JsonArray primary = groups.isEmpty() ? new JsonArray() : groups.getObject(0).getArray("thumbnails");
        final JsonArray thumbnails = primary.isEmpty()
                ? item.getObject("thumbnail").getArray("thumbnails")
                : primary;
        return getImagesFromThumbnailsArray(thumbnails);
    }

    @Override
    public String getName() throws ParsingException {
        final String name = getTextFromObject(item.getObject("title"));
        if (name == null || name.isBlank()) {
            throw new ParsingException("Playlist title is missing");
        }
        return name;
    }

    @Override
    public String getUrl() throws ParsingException {
        final String id = item.getString("playlistId");
        if (id == null || id.isBlank()) {
            throw new ParsingException("Playlist ID is missing");
        }
        return YoutubePlaylistLinkHandlerFactory.getInstance().getUrl(id);
    }

    @Override
    public String getUploaderName() throws ParsingException {
        return getTextFromObject(item.getObject("longBylineText"));
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        return getUrlFromObject(item.getObject("longBylineText"));
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return YoutubeParsingHelper.isVerified(item.getArray("ownerBadges"));
    }

    @Override
    public long getStreamCount() throws ParsingException {
        String count = item.getString("videoCount");
        if (count == null) count = getTextFromObject(item.getObject("videoCountText"));
        if (count == null) count = getTextFromObject(item.getObject("videoCountShortText"));
        if (count == null || count.isBlank()) {
            throw new ParsingException("Playlist item count is missing");
        }

        try {
            return containsMagnitude(count)
                    ? Utils.mixedNumberWordToLong(count)
                    : Long.parseLong(Utils.removeNonDigitCharacters(count));
        } catch (final NumberFormatException | ParsingException error) {
            throw new ParsingException("Could not parse playlist item count", error);
        }
    }

    private static boolean containsMagnitude(final String value) {
        final String upperCase = value.toUpperCase(Locale.ROOT);
        return upperCase.indexOf('K') >= 0 || upperCase.indexOf('M') >= 0 || upperCase.indexOf('B') >= 0;
    }
}
