// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCEventPayload;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper.getImageListFromLogoImageUrl;

public class MediaCCCRecentKioskExtractor implements StreamInfoItemExtractor {
    private static final String UTC_SUFFIX = "UTC";
    @Nullable private final String title;
    @Nullable private final String url;
    @Nullable private final String conferenceTitle;
    @Nullable private final String conferenceUrl;
    @Nullable private final String date;
    private final long duration;
    private final long viewCount;
    private final List<Image> thumbnails;

    public MediaCCCRecentKioskExtractor(final MediaCCCEventPayload event) {
        title = event.getTitle();
        url = event.getFrontendLink();
        conferenceTitle = event.getConferenceTitle();
        conferenceUrl = event.getConferenceUrl();
        date = event.getDate();
        duration = event.getDuration() == null ? 0 : event.getDuration();
        viewCount = event.getViewCount() == null ? 0 : event.getViewCount();
        thumbnails = getImageListFromLogoImageUrl(event.getPosterUrl());
    }

    @Override public String getName() throws ParsingException { return title; }
    @Override public String getUrl() throws ParsingException { return url; }
    @Nonnull @Override public List<Image> getThumbnails() { return thumbnails; }
    @Override public StreamType getStreamType() { return StreamType.VIDEO_STREAM; }
    @Override public boolean isAd() { return false; }
    @Override public long getDuration() { return duration; }
    @Override public long getViewCount() { return viewCount; }
    @Override public String getUploaderName() { return conferenceTitle; }
    @Override public String getUploaderUrl() throws ParsingException {
        return MediaCCCConferenceLinkHandlerFactory.getInstance().fromUrl(conferenceUrl).getUrl();
    }
    @Override public boolean isUploaderVerified() { return false; }
    @Nullable @Override public String getTextualUploadDate() { return date; }
    @Nullable @Override public DateWrapper getUploadDate() throws ParsingException {
        if (date == null) return null;
        // shortcut: API dates use UTC/offsets, use zone-aware parsing if named zones appear.
        final String isoDate = date.endsWith(UTC_SUFFIX)
                ? date.substring(0, date.length() - UTC_SUFFIX.length()) + "Z" : date;
        return DateWrapper.fromInstant(isoDate);
    }
}
