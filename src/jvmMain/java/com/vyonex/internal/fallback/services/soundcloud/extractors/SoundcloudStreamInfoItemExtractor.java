// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudProfileInfoItem;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudTrackInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nullable;
import java.util.List;

public class SoundcloudStreamInfoItemExtractor implements StreamInfoItemExtractor {
    private final String url;
    private final String name;
    private final long duration;
    private final String uploaderName;
    private final String uploaderUrl;
    private final String uploaderAvatarUrl;
    private final boolean uploaderVerified;
    private final String uploadDate;
    private final long viewCount;
    @Nullable private final String artworkUrl;

    public SoundcloudStreamInfoItemExtractor(final JsonObject track) {
        final JsonObject user = track.getObject("user");
        url = SoundcloudParsingHelper.toHttpsUrl(track.getString("permalink_url", ""));
        name = track.getString("title", "");
        duration = track.getLong("duration") / 1000;
        uploaderName = user.getString("username", "");
        uploaderUrl = SoundcloudParsingHelper.toHttpsUrl(user.getString("permalink_url", ""));
        uploaderAvatarUrl = user.getString("avatar_url");
        uploaderVerified = user.getBoolean("verified");
        uploadDate = track.getString("created_at", "");
        viewCount = track.getLong("playback_count");
        artworkUrl = track.getString("artwork_url", uploaderAvatarUrl);
    }

    public SoundcloudStreamInfoItemExtractor(final SoundcloudTrackInfoItem track) {
        final SoundcloudProfileInfoItem user = track.getUser();
        url = SoundcloudParsingHelper.toHttpsUrl(track.getPermalinkUrl());
        name = track.getTitle();
        duration = track.getDurationMillis() / 1000;
        uploaderName = user.getName();
        uploaderUrl = SoundcloudParsingHelper.toHttpsUrl(user.getPermalinkUrl());
        uploaderAvatarUrl = user.getAvatarUrl();
        uploaderVerified = user.getVerified();
        uploadDate = track.getCreatedAt();
        viewCount = track.getPlaybackCount();
        artworkUrl = track.getArtworkUrl();
    }

    @Override public String getUrl() { return url; }
    @Override public String getName() { return name; }
    @Override public long getDuration() { return duration; }
    @Override public String getUploaderName() { return uploaderName; }
    @Override public String getUploaderUrl() { return uploaderUrl; }
    @Override public List<Image> getUploaderAvatars() {
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(uploaderAvatarUrl);
    }
    @Override public boolean isUploaderVerified() { return uploaderVerified; }
    @Override public String getTextualUploadDate() { return uploadDate; }
    @Override public DateWrapper getUploadDate() throws ParsingException {
        return SoundcloudParsingHelper.parseDate(uploadDate);
    }
    @Override public long getViewCount() { return viewCount; }
    @Override public List<Image> getThumbnails() throws ParsingException {
        if (artworkUrl == null || artworkUrl.isBlank()) throw new ParsingException("Track has no artwork");
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(artworkUrl);
    }
    @Override public StreamType getStreamType() { return StreamType.AUDIO_STREAM; }
    @Override public boolean isAd() { return false; }
}
