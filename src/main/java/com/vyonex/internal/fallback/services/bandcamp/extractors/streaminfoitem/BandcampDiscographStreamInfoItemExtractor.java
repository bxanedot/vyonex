// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BandcampDiscographStreamInfoItemExtractor extends BandcampStreamInfoItemExtractor {
    private final String bandName;
    @Nullable private final String title;
    private final long bandId;
    private final long itemId;
    @Nullable private final String itemType;
    private final long artId;

    public BandcampDiscographStreamInfoItemExtractor(final JsonObject item,
                                                     final String uploaderUrl) {
        super(uploaderUrl);
        bandName = item.getString("band_name", "");
        title = item.getString("title");
        bandId = item.getLong("band_id");
        itemId = item.getLong("item_id");
        itemType = item.getString("item_type");
        artId = item.getLong("art_id");
    }

    @Override public String getUploaderName() { return bandName; }

    @Override
    public String getName() throws ParsingException {
        if (title == null || title.isBlank()) throw new ParsingException("Bandcamp track has no title");
        return title;
    }

    @Override
    public String getUrl() throws ParsingException {
        return BandcampExtractorHelper.getStreamUrlFromIds(bandId, itemId, itemType);
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageId(artId, true);
    }

    @Override
    public long getDuration() { return -1; }
}
