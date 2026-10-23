// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.List;

public class BandcampAlbumInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final JsonObject album;
    private final String uploaderUrl;

    public BandcampAlbumInfoItemExtractor(final JsonObject album, final String uploaderUrl) {
        this.album = album;
        this.uploaderUrl = uploaderUrl;
    }

    @Override
    public String getName() throws ParsingException { return required("title"); }

    @Override
    public String getUrl() throws ParsingException {
        return BandcampExtractorHelper.getStreamUrlFromIds(album.getLong("band_id"),
                album.getLong("item_id"), album.getString("item_type"));
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageId(album.getLong("art_id"), true);
    }

    @Override
    public String getUploaderName() { return album.getString("band_name", ""); }

    @Override
    public String getUploaderUrl() { return uploaderUrl; }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Override
    public long getStreamCount() { return ListExtractor.ITEM_COUNT_UNKNOWN; }

    private String required(final String key) throws ParsingException {
        final String value = album.getString(key);
        if (value == null || value.isBlank()) {
            throw new ParsingException("Bandcamp album item is missing " + key);
        }
        return value;
    }
}
