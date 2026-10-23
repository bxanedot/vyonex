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
    private final long showId;
    @Nullable private final String description;
    @Nullable private final String uploadDate;
    private final String name;
    private final long imageId;
    private final String uploaderName;

    public BandcampRadioInfoItemExtractor(final JsonObject show) {
        showId = show.getLong("id");
        description = show.getString("desc");
        uploadDate = show.getString("date");
        name = show.getString("subtitle");
        imageId = show.getLong("image_id");
        uploaderName = show.getString("title", "");
    }

    @Override
    public long getDuration() { return 0; }

    @Nullable
    @Override
    public String getShortDescription() { return description; }

    @Nullable
    @Override
    public String getTextualUploadDate() { return uploadDate; }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        final String date = getTextualUploadDate();
        return date == null || date.isBlank() ? null : BandcampExtractorHelper.parseDate(date);
    }

    @Override
    public String getName() throws ParsingException { return required(); }

    @Override
    public String getUrl() { return BandcampExtractorHelper.BASE_URL + "/?show=" + showId; }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageId(imageId, false);
    }

    @Override
    public StreamType getStreamType() { return StreamType.AUDIO_STREAM; }

    @Override
    public long getViewCount() { return -1; }

    @Override
    public String getUploaderName() { return uploaderName; }

    @Override
    public String getUploaderUrl() { return ""; }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Override
    public boolean isAd() { return false; }

    private String required() throws ParsingException {
        if (name == null || name.isBlank()) {
            throw new ParsingException("Bandcamp radio item is missing subtitle");
        }
        return name;
    }
}
