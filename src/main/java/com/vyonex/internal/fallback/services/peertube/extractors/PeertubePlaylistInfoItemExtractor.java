// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.json.JsonObject;

import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.requiredString;

public final class PeertubePlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final JsonObject item;
    private final String baseUrl;

    public PeertubePlaylistInfoItemExtractor(final JsonObject item, final String baseUrl) {
        this.item = item;
        this.baseUrl = baseUrl;
    }

    @Override public String getName() throws ParsingException {
        return requiredString(item, "displayName");
    }

    @Override public String getUrl() throws ParsingException {
        return requiredString(item, "url");
    }

    @Override public List<Image> getThumbnails() {
        return getThumbnailsFromPlaylistOrVideoItem(baseUrl, item);
    }

    @Override public String getUploaderName() throws ParsingException {
        return requiredString(item, "uploader.displayName");
    }

    @Override public String getUploaderUrl() throws ParsingException {
        return requiredString(item, "uploader.url");
    }

    @Override public boolean isUploaderVerified() { return false; }

    @Override public long getStreamCount() { return item.getLong("videosLength", -1); }

    @Override public Description getDescription() {
        final String description = item.getString("description");
        return description == null || description.isEmpty()
                ? Description.EMPTY_DESCRIPTION
                : new Description(description, Description.PLAIN_TEXT);
    }
}
