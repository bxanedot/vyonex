// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper
        .getImageListFromLogoImageUrl;

public final class MediaCCCRecentKioskExtractor implements StreamInfoItemExtractor {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSzzzz");

    private final JsonObject event;

    public MediaCCCRecentKioskExtractor(final JsonObject event) {
        this.event = event;
    }

    @Override
    public String getName() throws ParsingException { return event.getString("title"); }

    @Override
    public String getUrl() throws ParsingException { return event.getString("frontend_link"); }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return getImageListFromLogoImageUrl(event.getString("poster_url"));
    }

    @Override
    public StreamType getStreamType() { return StreamType.VIDEO_STREAM; }

    @Override
    public boolean isAd() { return false; }

    @Override
    public long getDuration() { return event.getInt("duration"); }

    @Override
    public long getViewCount() { return event.getInt("view_count"); }

    @Override
    public String getUploaderName() { return event.getString("conference_title"); }

    @Override
    public String getUploaderUrl() throws ParsingException {
        return MediaCCCConferenceLinkHandlerFactory.getInstance()
                .fromUrl(event.getString("conference_url")).getUrl();
    }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Nullable
    @Override
    public String getTextualUploadDate() { return event.getString("date"); }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return new DateWrapper(ZonedDateTime.parse(getTextualUploadDate(), DATE_FORMAT).toInstant());
    }
}
