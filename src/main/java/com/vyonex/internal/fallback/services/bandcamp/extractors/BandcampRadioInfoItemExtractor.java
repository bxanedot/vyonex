// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BandcampRadioInfoItemExtractor implements StreamInfoItemExtractor {
    private final JsonObject show;

    public BandcampRadioInfoItemExtractor(final JsonObject show) {
        this.show = show;
    }

    @Override
    public long getDuration() { return 0; }

    @Nullable
    @Override
    public String getShortDescription() { return show.getString("desc"); }

    @Nullable
    @Override
    public String getTextualUploadDate() { return show.getString("date"); }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        final String date = getTextualUploadDate();
        return date == null || date.isBlank() ? null : BandcampExtractorHelper.parseDate(date);
    }

    @Override
    public String getName() throws ParsingException { return required("subtitle"); }

    @Override
    public String getUrl() { return BandcampExtractorHelper.BASE_URL + "/?show=" + show.getLong("id"); }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageId(show.getLong("image_id"), false);
    }

    @Override
    public StreamType getStreamType() { return StreamType.AUDIO_STREAM; }

    @Override
    public long getViewCount() { return -1; }

    @Override
    public String getUploaderName() { return show.getString("title", ""); }

    @Override
    public String getUploaderUrl() { return ""; }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Override
    public boolean isAd() { return false; }

    private String required(final String key) throws ParsingException {
        final String value = show.getString(key);
        if (value == null || value.isBlank()) {
            throw new ParsingException("Bandcamp radio item is missing " + key);
        }
        return value;
    }
}
