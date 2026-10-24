// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudProfileInfoItem;

import javax.annotation.Nullable;
import java.util.List;

public final class SoundcloudChannelInfoItemExtractor implements ChannelInfoItemExtractor {
    private final String name;
    private final String url;
    @Nullable private final String avatarUrl;
    private final long followers;
    private final long tracks;
    private final boolean verified;
    private final String description;

    public SoundcloudChannelInfoItemExtractor(final SoundcloudProfileInfoItem user) {
        name = user.getName();
        url = SoundcloudParsingHelper.toHttpsUrl(user.getPermalinkUrl());
        avatarUrl = user.getAvatarUrl();
        followers = user.getFollowers();
        tracks = user.getTrackCount();
        verified = user.getVerified();
        description = user.getDescription();
    }

    @Override public String getName() { return name; }
    @Override public String getUrl() { return url; }
    @Override public List<Image> getThumbnails() {
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(avatarUrl);
    }
    @Override public long getSubscriberCount() { return followers; }
    @Override public long getStreamCount() { return tracks; }
    @Override public boolean isVerified() { return verified; }
    @Override public String getDescription() { return description; }
}
