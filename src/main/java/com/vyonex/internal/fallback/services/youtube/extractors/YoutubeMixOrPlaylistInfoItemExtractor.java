// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfo;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.extractPlaylistTypeFromPlaylistUrl;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getThumbnailsFromInfoItem;

/** Maps compact YouTube playlist and mix cards into VyonEx playlist items. */
public class YoutubeMixOrPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final JsonObject item;

    public YoutubeMixOrPlaylistInfoItemExtractor(final JsonObject item) {
        this.item = Objects.requireNonNull(item, "playlist card is required");
    }

    @Override
    public String getName() throws ParsingException {
        final String name = getTextFromObject(item.getObject("title"));
        if (name == null || name.isBlank()) {
            throw new ParsingException("Playlist card title is missing");
        }
        return name;
    }

    @Override
    public String getUrl() throws ParsingException {
        final String url = item.getString("shareUrl");
        if (url == null || url.isBlank()) {
            throw new ParsingException("Playlist card URL is missing");
        }
        return url;
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        return getThumbnailsFromInfoItem(item);
    }

    @Override
    public String getUploaderName() {
        return getTextFromObject(item.getObject("longBylineText"));
    }

    @Override
    public String getUploaderUrl() {
        return null;
    }

    @Override
    public boolean isUploaderVerified() {
        return false;
    }

    @Override
    public long getStreamCount() throws ParsingException {
        final String count = getTextFromObject(item.getObject("videoCountShortText"));
        if (count == null || count.isBlank()) {
            throw new ParsingException("Playlist card item count is missing");
        }
        if (count.strip().endsWith("+")) {
            return ListExtractor.ITEM_COUNT_INFINITE;
        }
        try {
            return Utils.mixedNumberWordToLong(count);
        } catch (final NumberFormatException | ParsingException error) {
            throw new ParsingException("Could not parse playlist card item count", error);
        }
    }

    @Nonnull
    @Override
    public PlaylistInfo.PlaylistType getPlaylistType() throws ParsingException {
        return extractPlaylistTypeFromPlaylistUrl(getUrl());
    }
}
