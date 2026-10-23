// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonObject;

import java.util.List;

public class SoundcloudStreamInfoItemExtractor implements StreamInfoItemExtractor {
    private final JsonObject track;

    public SoundcloudStreamInfoItemExtractor(final JsonObject track) { this.track = track; }

    @Override public String getUrl() { return SoundcloudParsingHelper.toHttpsUrl(track.getString("permalink_url", "")); }
    @Override public String getName() { return track.getString("title", ""); }
    @Override public long getDuration() { return track.getLong("duration") / 1000; }
    @Override public String getUploaderName() { return track.getObject("user").getString("username", ""); }
    @Override public String getUploaderUrl() {
        return SoundcloudParsingHelper.toHttpsUrl(track.getObject("user").getString("permalink_url", ""));
    }
    @Override public List<Image> getUploaderAvatars() {
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(
                track.getObject("user").getString("avatar_url"));
    }
    @Override public boolean isUploaderVerified() { return track.getObject("user").getBoolean("verified"); }
    @Override public String getTextualUploadDate() { return track.getString("created_at", ""); }
    @Override public DateWrapper getUploadDate() throws ParsingException {
        return SoundcloudParsingHelper.parseDate(getTextualUploadDate());
    }
    @Override public long getViewCount() { return track.getLong("playback_count"); }
    @Override public List<Image> getThumbnails() throws ParsingException {
        return SoundcloudParsingHelper.getAllImagesFromTrackObject(track);
    }
    @Override public StreamType getStreamType() { return StreamType.AUDIO_STREAM; }
    @Override public boolean isAd() { return false; }
}
