// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonObject;

import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.requiredString;

public final class PeertubeChannelInfoItemExtractor implements ChannelInfoItemExtractor {
    private final JsonObject item;
    private final String baseUrl;

    public PeertubeChannelInfoItemExtractor(final JsonObject item, final String baseUrl) {
        this.item = item;
        this.baseUrl = baseUrl;
    }

    @Override public String getName() throws ParsingException { return requiredString(item, "displayName"); }
    @Override public String getUrl() throws ParsingException { return requiredString(item, "url"); }
    @Override public List<Image> getThumbnails() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, item);
    }
    @Override public String getDescription() { return item.getString("description"); }
    @Override public long getSubscriberCount() { return item.getLong("followersCount", -1); }
    @Override public long getStreamCount() { return ListExtractor.ITEM_COUNT_UNKNOWN; }
    @Override public boolean isVerified() { return false; }
}
