// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.json.JsonObject;

import java.util.List;

public final class SoundcloudChannelInfoItemExtractor implements ChannelInfoItemExtractor {
    private final JsonObject user;

    public SoundcloudChannelInfoItemExtractor(final JsonObject user) { this.user = user; }

    @Override public String getName() { return user.getString("username", ""); }
    @Override public String getUrl() {
        return SoundcloudParsingHelper.toHttpsUrl(user.getString("permalink_url", ""));
    }
    @Override public List<Image> getThumbnails() {
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(
                user.getString("avatar_url"));
    }
    @Override public long getSubscriberCount() { return user.getLong("followers_count"); }
    @Override public long getStreamCount() { return user.getLong("track_count"); }
    @Override public boolean isVerified() { return user.getBoolean("verified"); }
    @Override public String getDescription() { return user.getString("description", ""); }
}
