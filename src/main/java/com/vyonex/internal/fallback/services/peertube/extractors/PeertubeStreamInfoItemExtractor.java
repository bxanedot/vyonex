// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ServiceList;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonObject;

import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.optionalString;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.requiredString;

public class PeertubeStreamInfoItemExtractor implements StreamInfoItemExtractor {
    protected final JsonObject item;
    private String baseUrl;

    public PeertubeStreamInfoItemExtractor(final JsonObject item, final String baseUrl) {
        this.item = item;
        this.baseUrl = baseUrl;
    }

    @Override public String getUrl() throws ParsingException {
        return ServiceList.PeerTube.getStreamLHFactory()
                .fromId(requiredString(item, "uuid"), baseUrl).getUrl();
    }

    @Override public List<Image> getThumbnails() {
        return getThumbnailsFromPlaylistOrVideoItem(baseUrl, item);
    }

    @Override public String getName() throws ParsingException {
        return requiredString(item, "name");
    }

    @Override public boolean isAd() { return false; }

    @Override public long getViewCount() { return item.getLong("views", -1); }

    @Override public String getUploaderUrl() throws ParsingException {
        final String account = requiredString(item, "account.name");
        final String host = requiredString(item, "account.host");
        return ServiceList.PeerTube.getChannelLHFactory()
                .fromId("accounts/" + account + "@" + host, baseUrl).getUrl();
    }

    @Override public List<Image> getUploaderAvatars() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl,
                item.getObject("account"));
    }

    @Override public boolean isUploaderVerified() { return false; }

    @Override public String getUploaderName() throws ParsingException {
        return requiredString(item, "account.displayName");
    }

    @Override public String getTextualUploadDate() { return optionalString(item, "publishedAt"); }

    @Override public DateWrapper getUploadDate() throws ParsingException {
        final String date = getTextualUploadDate();
        return date == null ? null : DateWrapper.fromInstant(date);
    }

    @Override public StreamType getStreamType() {
        return item.getBoolean("isLive") ? StreamType.LIVE_STREAM : StreamType.VIDEO_STREAM;
    }

    @Override public long getDuration() { return item.getLong("duration", -1); }

    protected void setBaseUrl(final String baseUrl) { this.baseUrl = baseUrl; }
}
