// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nullable;
import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem;

public final class PeertubePlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    @Nullable private final String displayName;
    @Nullable private final String url;
    @Nullable private final String uploaderName;
    @Nullable private final String uploaderUrl;
    @Nullable private final String description;
    private final long streamCount;
    private final List<Image> thumbnails;

    public PeertubePlaylistInfoItemExtractor(final JsonObject item, final String baseUrl) {
        displayName = item.getString("displayName");
        url = item.getString("url");
        final JsonObject uploader = item.getObject("uploader");
        uploaderName = uploader.getString("displayName");
        uploaderUrl = uploader.getString("url");
        description = item.getString("description");
        streamCount = item.getLong("videosLength", -1);
        thumbnails = getThumbnailsFromPlaylistOrVideoItem(baseUrl, item);
    }

    @Override public String getName() throws ParsingException {
        return required(displayName, "displayName");
    }
    @Override public String getUrl() throws ParsingException { return required(url, "url"); }
    @Override public List<Image> getThumbnails() { return thumbnails; }
    @Override public String getUploaderName() throws ParsingException {
        return required(uploaderName, "uploader.displayName");
    }
    @Override public String getUploaderUrl() throws ParsingException {
        return required(uploaderUrl, "uploader.url");
    }
    @Override public boolean isUploaderVerified() { return false; }
    @Override public long getStreamCount() { return streamCount; }
    @Override public Description getDescription() {
        return description == null || description.isEmpty()
                ? Description.EMPTY_DESCRIPTION : new Description(description, Description.PLAIN_TEXT);
    }

    private static String required(@Nullable final String value, final String field)
            throws ParsingException {
        if (value == null) throw new ParsingException("Missing string field: " + field);
        return value;
    }
}
