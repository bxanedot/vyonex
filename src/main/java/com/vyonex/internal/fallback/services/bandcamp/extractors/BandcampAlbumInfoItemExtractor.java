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
    private final String title;
    private final long bandId;
    private final long itemId;
    private final String itemType;
    private final long artworkId;
    private final String uploaderName;
    private final String uploaderUrl;

    public BandcampAlbumInfoItemExtractor(final JsonObject album, final String uploaderUrl) {
        title = album.getString("title");
        bandId = album.getLong("band_id");
        itemId = album.getLong("item_id");
        itemType = album.getString("item_type");
        artworkId = album.getLong("art_id");
        uploaderName = album.getString("band_name", "");
        this.uploaderUrl = uploaderUrl;
    }

    @Override
    public String getName() throws ParsingException {
        if (title == null || title.isBlank()) {
            throw new ParsingException("Bandcamp album item is missing title");
        }
        return title;
    }

    @Override
    public String getUrl() throws ParsingException {
        return BandcampExtractorHelper.getStreamUrlFromIds(bandId, itemId, itemType);
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageId(artworkId, true);
    }

    @Override
    public String getUploaderName() { return uploaderName; }

    @Override
    public String getUploaderUrl() { return uploaderUrl; }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Override
    public long getStreamCount() { return ListExtractor.ITEM_COUNT_UNKNOWN; }
}
