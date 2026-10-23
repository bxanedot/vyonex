// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.json.JsonObject;

import java.util.List;

public final class SoundcloudPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final JsonObject playlist;

    public SoundcloudPlaylistInfoItemExtractor(final JsonObject playlist) { this.playlist = playlist; }

    @Override public String getName() { return playlist.getString("title", ""); }
    @Override public String getUrl() {
        return SoundcloudParsingHelper.toHttpsUrl(playlist.getString("permalink_url", ""));
    }
    @Override public List<Image> getThumbnails() throws ParsingException {
        String url = playlist.getString("artwork_url");
        if (url == null || url.isBlank()) {
            for (final Object value : playlist.getArray("tracks")) {
                if (!(value instanceof JsonObject)) continue;
                final JsonObject track = (JsonObject) value;
                url = track.getString("artwork_url");
                if (url == null || url.isBlank()) {
                    url = track.getObject("user").getString("avatar_url");
                }
                if (url != null && !url.isBlank()) break;
            }
        }
        if (url == null || url.isBlank()) {
            url = playlist.getObject("user").getString("avatar_url");
        }
        if (url == null || url.isBlank()) throw new ParsingException("Playlist has no image");
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(url);
    }
    @Override public String getUploaderName() {
        return playlist.getObject("user").getString("username", "");
    }
    @Override public String getUploaderUrl() {
        return SoundcloudParsingHelper.toHttpsUrl(
                playlist.getObject("user").getString("permalink_url", ""));
    }
    @Override public boolean isUploaderVerified() {
        return playlist.getObject("user").getBoolean("verified");
    }
    @Override public long getStreamCount() { return playlist.getLong("track_count"); }
}
