// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.List;

public class BandcampDiscographStreamInfoItemExtractor extends BandcampStreamInfoItemExtractor {
    private final JsonObject item;

    public BandcampDiscographStreamInfoItemExtractor(final JsonObject item,
                                                     final String uploaderUrl) {
        super(uploaderUrl);
        this.item = item;
    }

    @Override
    public String getUploaderName() { return item.getString("band_name", ""); }

    @Override
    public String getName() throws ParsingException {
        final String title = item.getString("title");
        if (title == null || title.isBlank()) throw new ParsingException("Bandcamp track has no title");
        return title;
    }

    @Override
    public String getUrl() throws ParsingException {
        return BandcampExtractorHelper.getStreamUrlFromIds(item.getLong("band_id"),
                item.getLong("item_id"), item.getString("item_type"));
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageId(item.getLong("art_id"), true);
    }

    @Override
    public long getDuration() { return -1; }
}
