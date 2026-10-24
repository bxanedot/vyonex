// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudPlaylistInfoItem;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudProfileInfoItem;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nullable;
import java.util.List;

public final class SoundcloudPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final String title;
    private final String url;
    private final String uploaderName;
    private final String uploaderUrl;
    @Nullable private final String avatarUrl;
    private final boolean uploaderVerified;
    private final long trackCount;
    @Nullable private final String artworkUrl;

    public SoundcloudPlaylistInfoItemExtractor(final JsonObject playlist) {
        final JsonObject user = playlist.getObject("user");
        title = playlist.getString("title", "");
        url = SoundcloudParsingHelper.toHttpsUrl(playlist.getString("permalink_url", ""));
        uploaderName = user.getString("username", "");
        uploaderUrl = SoundcloudParsingHelper.toHttpsUrl(user.getString("permalink_url", ""));
        avatarUrl = user.getString("avatar_url");
        uploaderVerified = user.getBoolean("verified");
        trackCount = playlist.getLong("track_count");
        artworkUrl = artworkUrl(playlist, avatarUrl);
    }

    public SoundcloudPlaylistInfoItemExtractor(final SoundcloudPlaylistInfoItem playlist) {
        final SoundcloudProfileInfoItem user = playlist.getUser();
        title = playlist.getTitle();
        url = SoundcloudParsingHelper.toHttpsUrl(playlist.getPermalinkUrl());
        uploaderName = user.getName();
        uploaderUrl = SoundcloudParsingHelper.toHttpsUrl(user.getPermalinkUrl());
        avatarUrl = user.getAvatarUrl();
        uploaderVerified = user.getVerified();
        trackCount = playlist.getTrackCount();
        artworkUrl = playlist.getArtworkUrl();
    }

    private static String artworkUrl(final JsonObject playlist, @Nullable final String avatarUrl) {
        String value = playlist.getString("artwork_url");
        if (value == null || value.isBlank()) {
            for (final Object item : playlist.getArray("tracks")) {
                if (!(item instanceof JsonObject track)) continue;
                value = track.getString("artwork_url");
                if (value == null || value.isBlank()) {
                    value = track.getObject("user").getString("avatar_url");
                }
                if (value != null && !value.isBlank()) return value;
            }
        }
        return value == null || value.isBlank() ? avatarUrl : value;
    }

    @Override public String getName() { return title; }
    @Override public String getUrl() { return url; }
    @Override public List<Image> getThumbnails() throws ParsingException {
        if (artworkUrl == null || artworkUrl.isBlank()) throw new ParsingException("Playlist has no image");
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(artworkUrl);
    }
    @Override public String getUploaderName() { return uploaderName; }
    @Override public String getUploaderUrl() { return uploaderUrl; }
    @Override public boolean isUploaderVerified() { return uploaderVerified; }
    @Override public long getStreamCount() { return trackCount; }
}
