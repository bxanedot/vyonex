// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getUrlFromNavigationEndpoint;
import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS;

/** Converts YouTube Music album and playlist search rows into playlist items. */
public class YoutubeMusicAlbumOrPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final JsonObject item;
    private final JsonObject uploader;

    public YoutubeMusicAlbumOrPlaylistInfoItemExtractor(final JsonObject item,
                                                        final JsonArray descriptionElements,
                                                        final String searchType) {
        this.item = Objects.requireNonNull(item, "music row is required");
        Objects.requireNonNull(descriptionElements, "description elements are required");
        final int uploaderIndex = MUSIC_ALBUMS.equals(searchType) ? 2 : 0;
        this.uploader = descriptionElements.getObject(uploaderIndex);
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

    @Override
    public String getName() throws ParsingException {
        final JsonArray columns = item.getArray("flexColumns");
        final String title = columns.isEmpty() ? null : getTextFromObject(columns.getObject(0)
                .getObject("musicResponsiveListItemFlexColumnRenderer").getObject("text"));
        if (title == null || title.isBlank()) throw new ParsingException("Music item title is missing");
        return title;
    }

    @Override
    public String getUrl() throws ParsingException {
        String playlistId = null;
        for (final JsonObject menuItem : item.getObject("menu").getObject("menuRenderer")
                .getArray("items").streamAsJsonObjects().toList()) {
            playlistId = menuItem.getObject("toggleMenuServiceItemRenderer")
                    .getObject("toggledServiceEndpoint").getObject("likeEndpoint")
                    .getObject("target").getString("playlistId");
            if (playlistId != null && !playlistId.isBlank()) break;
        }
        if (playlistId == null || playlistId.isBlank()) {
            playlistId = item.getObject("overlay").getObject("musicItemThumbnailOverlayRenderer")
                    .getObject("content").getObject("musicPlayButtonRenderer")
                    .getObject("playNavigationEndpoint").getObject("watchPlaylistEndpoint")
                    .getString("playlistId");
        }
        if (playlistId == null || playlistId.isBlank()) {
            throw new ParsingException("Music playlist ID is missing");
        }
        return "https://music.youtube.com/playlist?list=" + playlistId;
    }

    @Override
    public String getUploaderName() throws ParsingException {
        final String name = uploader.getString("text");
        if (name == null || name.isBlank()) throw new ParsingException("Uploader name is missing");
        return name;
    }

    @Nullable
    @Override
    public String getUploaderUrl() {
        for (final JsonObject menuItem : item.getObject("menu").getObject("menuRenderer")
                .getArray("items").streamAsJsonObjects().toList()) {
            final JsonObject artist = menuItem.getObject("menuNavigationItemRenderer");
            if ("ARTIST".equals(artist.getObject("icon").getString("iconType"))) {
                final String artistUrl = getUrlFromNavigationEndpoint(
                        artist.getObject("navigationEndpoint"));
                if (artistUrl != null) return artistUrl;
            }
        }
        return uploader.has("navigationEndpoint")
                ? getUrlFromNavigationEndpoint(uploader.getObject("navigationEndpoint")) : null;
    }

    @Override public boolean isUploaderVerified() { return false; }
    @Override public long getStreamCount() { return ListExtractor.ITEM_COUNT_UNKNOWN; }
}
