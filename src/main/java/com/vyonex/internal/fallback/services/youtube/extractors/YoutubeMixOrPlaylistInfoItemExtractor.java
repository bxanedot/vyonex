// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfo;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

public class YoutubeMixOrPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private static final Pattern PLUS_COUNT = Pattern.compile("^[\\d.,]+\\s*\\+.*$");
    private final JsonObject item;

    public YoutubeMixOrPlaylistInfoItemExtractor(final JsonObject item) {
        if (item == null) throw new IllegalArgumentException("Playlist item is required");
        this.item = item;
    }

    @Override
    public String getName() throws ParsingException {
        final String name = YoutubeParsingHelper.getTextFromObject(item.getObject("title"));
        if (name == null || name.isBlank()) throw new ParsingException("Playlist title is missing");
        return name;
    }

    @Override
    public String getUrl() throws ParsingException {
        final String value = item.getString("shareUrl");
        if (value == null || value.isBlank()) throw new ParsingException("Playlist card URL is missing");
        try {
            final URI uri = URI.create(value);
            final String host = uri.getHost();
            final String path = uri.getPath();
            final boolean youtubeHost = host != null && (host.equalsIgnoreCase("youtube.com")
                    || host.equalsIgnoreCase("www.youtube.com")
                    || host.equalsIgnoreCase("m.youtube.com")
                    || host.equalsIgnoreCase("music.youtube.com")
                    || host.equalsIgnoreCase("www.music.youtube.com"));
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !youtubeHost
                    || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                    || uri.getRawQuery() == null
                    || !("/watch".equals(path) || "/playlist".equals(path))) {
                throw new ParsingException("Playlist card URL is not a YouTube watch or playlist URL");
            }
            return value;
        } catch (IllegalArgumentException error) {
            throw new ParsingException("Playlist card URL is invalid", error);
        }
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        final JsonArray thumbnails = item.getObject("thumbnail").getArray("thumbnails");
        return YoutubeParsingHelper.getImagesFromThumbnailsArray(thumbnails);
    }

    @Override
    public String getUploaderName() {
        return YoutubeParsingHelper.getTextFromObject(item.getObject("longBylineText"));
    }

    @Override
    public String getUploaderUrl() {
        return YoutubeParsingHelper.getUrlFromObject(item.getObject("longBylineText"));
    }

    @Override
    public boolean isUploaderVerified() {
        return false;
    }

    @Override
    public long getStreamCount() throws ParsingException {
        final String value = YoutubeParsingHelper.getTextFromObject(item.getObject("videoCountShortText"));
        if (value == null || value.isBlank()) throw new ParsingException("Playlist count is missing");
        if (PLUS_COUNT.matcher(value.trim()).matches()) return ListExtractor.ITEM_COUNT_INFINITE;
        try {
            return Utils.mixedNumberWordToLong(value);
        } catch (NumberFormatException error) {
            throw new ParsingException("Playlist count is invalid", error);
        }
    }

    @Override
    public PlaylistInfo.PlaylistType getPlaylistType() {
        return PlaylistInfo.PlaylistType.MIX_STREAM;
    }
}
