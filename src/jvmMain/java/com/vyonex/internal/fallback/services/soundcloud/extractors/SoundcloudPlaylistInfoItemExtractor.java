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
