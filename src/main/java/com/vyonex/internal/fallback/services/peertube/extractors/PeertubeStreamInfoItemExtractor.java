// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ServiceList;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nullable;
import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem;

public class PeertubeStreamInfoItemExtractor implements StreamInfoItemExtractor {
    @Nullable private final String uuid;
    @Nullable private final String name;
    @Nullable protected final String embedUrl;
    private final JsonObject account;
    @Nullable private final String thumbnailPath;
    @Nullable private final String previewPath;
    @Nullable private final String publishedAt;
    private final long views;
    private final long duration;
    private final boolean live;
    private String baseUrl;

    public PeertubeStreamInfoItemExtractor(final JsonObject item, final String baseUrl) {
        uuid = item.getString("uuid");
        name = item.getString("name");
        embedUrl = item.getString("embedUrl");
        account = item.getObject("account");
        thumbnailPath = item.getString("thumbnailPath");
        previewPath = item.getString("previewPath");
        publishedAt = item.getString("publishedAt");
        views = item.getLong("views", -1);
        duration = item.getLong("duration", -1);
        live = item.getBoolean("isLive");
        this.baseUrl = baseUrl;
    }

    @Override public String getUrl() throws ParsingException {
        return ServiceList.PeerTube.getStreamLHFactory()
                .fromId(required(uuid, "uuid"), baseUrl).getUrl();
    }
    @Override public List<Image> getThumbnails() {
        return getThumbnailsFromPlaylistOrVideoItem(baseUrl, thumbnailPath, previewPath);
    }
    @Override public String getName() throws ParsingException { return required(name, "name"); }
    @Override public boolean isAd() { return false; }
    @Override public long getViewCount() { return views; }
    @Override public String getUploaderUrl() throws ParsingException {
        final String accountName = required(account.getString("name"), "account.name");
        final String host = required(account.getString("host"), "account.host");
        final String id = "accounts/" + accountName + "@" + host;
        return ServiceList.PeerTube.getChannelLHFactory().fromId(id, baseUrl).getUrl();
    }
    @Override public List<Image> getUploaderAvatars() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, account);
    }
    @Override public boolean isUploaderVerified() { return false; }
    @Override public String getUploaderName() throws ParsingException {
        return required(account.getString("displayName"), "account.displayName");
    }
    @Override public String getTextualUploadDate() { return publishedAt; }
    @Override public DateWrapper getUploadDate() throws ParsingException {
        final String date = getTextualUploadDate();
        return date == null ? null : DateWrapper.fromInstant(date);
    }
    @Override public StreamType getStreamType() {
        return live ? StreamType.LIVE_STREAM : StreamType.VIDEO_STREAM;
    }
    @Override public long getDuration() { return duration; }

    protected void setBaseUrl(final String value) { baseUrl = value; }

    private static String required(@Nullable final String value, final String field)
            throws ParsingException {
        if (value == null) throw new ParsingException("Missing string field: " + field);
        return value;
    }
}
