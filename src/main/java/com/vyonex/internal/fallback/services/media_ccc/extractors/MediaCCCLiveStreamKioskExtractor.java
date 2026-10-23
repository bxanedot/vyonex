// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper.getThumbnailsFromLiveStreamItem;

public class MediaCCCLiveStreamKioskExtractor implements StreamInfoItemExtractor {
    @Nullable private final String title;
    @Nullable private final String link;
    private final String uploaderName;
    @Nullable private final String uploaderUrl;
    private final List<Image> thumbnails;
    private final StreamType streamType;

    public MediaCCCLiveStreamKioskExtractor(final JsonObject conference, final String group,
                                            final JsonObject room) {
        title = room.getObject("talks").getObject("current").getString("title");
        link = room.getString("link");
        uploaderName = conference.getString("conference") + " - " + group + " - "
                + room.getString("display");
        uploaderUrl = "https://media.ccc.de/c/" + conference.getString("slug");
        thumbnails = getThumbnailsFromLiveStreamItem(room);
        streamType = streamType(room.getArray("streams"));
    }

    private static StreamType streamType(final JsonArray streams) {
        for (final Object value : streams) {
            if (value instanceof JsonObject
                    && "video".equals(((JsonObject) value).getString("type"))) {
                return StreamType.LIVE_STREAM;
            }
        }
        return StreamType.AUDIO_LIVE_STREAM;
    }

    @Override public String getName() throws ParsingException { return title; }
    @Override public String getUrl() throws ParsingException { return link; }
    @Nonnull @Override public List<Image> getThumbnails() { return thumbnails; }
    @Override public StreamType getStreamType() { return streamType; }
    @Override public boolean isAd() { return false; }
    @Override public long getDuration() { return 0; }
    @Override public long getViewCount() { return -1; }
    @Override public String getUploaderName() { return uploaderName; }
    @Nullable @Override public String getUploaderUrl() { return uploaderUrl; }
    @Override public boolean isUploaderVerified() { return false; }
    @Nullable @Override public String getTextualUploadDate() { return null; }
    @Nullable @Override public DateWrapper getUploadDate() { return null; }
}
