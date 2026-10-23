// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
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
    private final JsonObject conference;
    private final String group;
    private final JsonObject room;

    public MediaCCCLiveStreamKioskExtractor(final JsonObject conference, final String group,
                                            final JsonObject room) {
        this.conference = conference;
        this.group = group;
        this.room = room;
    }

    @Override public String getName() throws ParsingException {
        return room.getObject("talks").getObject("current").getString("title");
    }
    @Override public String getUrl() throws ParsingException { return room.getString("link"); }
    @Nonnull @Override public List<Image> getThumbnails() {
        return getThumbnailsFromLiveStreamItem(room);
    }
    @Override public StreamType getStreamType() {
        final JsonArray streams = room.getArray("streams");
        for (final Object value : streams) {
            if (value instanceof JsonObject && "video".equals(((JsonObject) value).getString("type"))) {
                return StreamType.LIVE_STREAM;
            }
        }
        return StreamType.AUDIO_LIVE_STREAM;
    }
    @Override public boolean isAd() { return false; }
    @Override public long getDuration() { return 0; }
    @Override public long getViewCount() { return -1; }
    @Override public String getUploaderName() throws ParsingException {
        return conference.getString("conference") + " - " + group + " - "
                + room.getString("display");
    }
    @Override public String getUploaderUrl() throws ParsingException {
        return "https://media.ccc.de/c/" + conference.getString("slug");
    }
    @Override public boolean isUploaderVerified() { return false; }
    @Nullable @Override public String getTextualUploadDate() { return null; }
    @Nullable @Override public DateWrapper getUploadDate() { return null; }
}
