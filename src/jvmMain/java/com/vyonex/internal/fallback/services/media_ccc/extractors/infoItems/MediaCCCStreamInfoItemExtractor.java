// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors.infoItems;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCEventPayload;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper.getThumbnailsFromStreamItem;

public class MediaCCCStreamInfoItemExtractor implements StreamInfoItemExtractor {
    @Nullable private final String title;
    @Nullable private final String guid;
    @Nullable private final String conferenceTitle;
    @Nullable private final String conferenceUrl;
    @Nullable private final String releaseDate;
    private final long length;
    private final long viewCount;
    private final List<Image> thumbnails;

    public MediaCCCStreamInfoItemExtractor(final MediaCCCEventPayload event) {
        title = event.getTitle();
        guid = event.getGuid();
        conferenceTitle = event.getConferenceTitle();
        conferenceUrl = event.getConferenceUrl();
        releaseDate = event.getReleaseDate();
        length = event.getLength() == null ? 0 : event.getLength();
        viewCount = event.getViewCount() == null ? 0 : event.getViewCount();
        thumbnails = getThumbnailsFromStreamItem(event.getThumbUrl(), event.getPosterUrl());
    }

    @Override public StreamType getStreamType() { return StreamType.VIDEO_STREAM; }
    @Override public boolean isAd() { return false; }
    @Override public long getDuration() { return length; }
    @Override public long getViewCount() { return viewCount; }
    @Override public String getUploaderName() { return conferenceTitle; }
    @Override public String getUploaderUrl() { return conferenceUrl; }
    @Override public boolean isUploaderVerified() { return false; }
    @Nullable @Override public String getTextualUploadDate() { return releaseDate; }
    @Nullable @Override public DateWrapper getUploadDate() throws ParsingException {
        return DateWrapper.fromOffsetDateTime(releaseDate);
    }
    @Override public String getName() throws ParsingException { return required(title, "title"); }
    @Override public String getUrl() throws ParsingException {
        return "https://media.ccc.de/public/events/" + required(guid, "guid");
    }
    @Nonnull @Override public List<Image> getThumbnails() { return thumbnails; }

    private static String required(@Nullable final String value, final String key)
            throws ParsingException {
        if (value == null || value.isBlank()) {
            throw new ParsingException("MediaCCC event is missing " + key);
        }
        return value;
    }
}
