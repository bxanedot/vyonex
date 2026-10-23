// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nullable;
import java.util.List;

public final class PeertubeChannelInfoItemExtractor implements ChannelInfoItemExtractor {
    @Nullable private final String name;
    @Nullable private final String url;
    private final List<Image> thumbnails;
    @Nullable private final String description;
    private final long subscriberCount;

    public PeertubeChannelInfoItemExtractor(final JsonObject item, final String baseUrl) {
        name = item.getString("displayName");
        url = item.getString("url");
        thumbnails = PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, item);
        description = item.getString("description");
        subscriberCount = item.getLong("followersCount", -1);
    }

    @Override public String getName() throws ParsingException { return required(name, "displayName"); }
    @Override public String getUrl() throws ParsingException { return required(url, "url"); }
    @Override public List<Image> getThumbnails() { return thumbnails; }
    @Override public String getDescription() { return description; }
    @Override public long getSubscriberCount() { return subscriberCount; }
    @Override public long getStreamCount() { return ListExtractor.ITEM_COUNT_UNKNOWN; }
    @Override public boolean isVerified() { return false; }

    private static String required(@Nullable final String value, final String field)
            throws ParsingException {
        if (value == null) throw new ParsingException("Missing string field: " + field);
        return value;
    }
}
