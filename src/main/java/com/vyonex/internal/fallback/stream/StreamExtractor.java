// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import com.vyonex.internal.fallback.Extractor;
import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.InfoItemExtractor;
import com.vyonex.internal.fallback.InfoItemsCollector;
import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.MetaInfo;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.utils.Parser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Common extraction contract for audio and video pages. */
public abstract class StreamExtractor extends Extractor {
    public static final int NO_AGE_LIMIT = 0;
    public static final long UNKNOWN_SUBSCRIBER_COUNT = -1;

    public StreamExtractor(final StreamingService service, final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Nullable public String getTextualUploadDate() throws ParsingException { return null; }
    @Nullable public DateWrapper getUploadDate() throws ParsingException { return null; }
    @Nonnull public abstract List<Image> getThumbnails() throws ParsingException;
    @Nonnull public Description getDescription() throws ParsingException {
        return Description.EMPTY_DESCRIPTION;
    }
    public int getAgeLimit() throws ParsingException { return NO_AGE_LIMIT; }
    public long getLength() throws ParsingException { return 0; }
    public long getTimeStamp() throws ParsingException { return 0; }
    public long getViewCount() throws ParsingException { return -1; }
    public long getLikeCount() throws ParsingException { return -1; }
    public long getDislikeCount() throws ParsingException { return -1; }
    @Nonnull public abstract String getUploaderUrl() throws ParsingException;
    @Nonnull public abstract String getUploaderName() throws ParsingException;
    public boolean isUploaderVerified() throws ParsingException { return false; }
    public long getUploaderSubscriberCount() throws ParsingException {
        return UNKNOWN_SUBSCRIBER_COUNT;
    }
    @Nonnull public List<Image> getUploaderAvatars() throws ParsingException { return List.of(); }
    @Nonnull public String getSubChannelUrl() throws ParsingException { return ""; }
    @Nonnull public String getSubChannelName() throws ParsingException { return ""; }
    @Nonnull public List<Image> getSubChannelAvatars() throws ParsingException { return List.of(); }
    @Nonnull public String getDashMpdUrl() throws ParsingException { return ""; }
    @Nonnull public String getHlsUrl() throws ParsingException { return ""; }
    public abstract List<AudioStream> getAudioStreams() throws IOException, ExtractionException;
    public abstract List<VideoStream> getVideoStreams() throws IOException, ExtractionException;
    public abstract List<VideoStream> getVideoOnlyStreams() throws IOException, ExtractionException;
    @Nonnull public List<SubtitlesStream> getSubtitlesDefault()
            throws IOException, ExtractionException { return List.of(); }
    @Nonnull public List<SubtitlesStream> getSubtitles(final MediaFormat format)
            throws IOException, ExtractionException { return List.of(); }
    public abstract StreamType getStreamType() throws ParsingException;

    @Nullable
    public InfoItemsCollector<? extends InfoItem, ? extends InfoItemExtractor> getRelatedItems()
            throws IOException, ExtractionException {
        return null;
    }

    /** @deprecated Use {@link #getRelatedItems()}. */
    @Deprecated @Nullable
    public StreamInfoItemsCollector getRelatedStreams()
            throws IOException, ExtractionException {
        final InfoItemsCollector<?, ?> items = getRelatedItems();
        return items instanceof StreamInfoItemsCollector streams ? streams : null;
    }

    @Nonnull public List<Frameset> getFrames() throws ExtractionException { return List.of(); }
    @Nullable public String getErrorMessage() { return null; }

    protected long getTimestampSeconds(final String regexPattern) throws ParsingException {
        final String timestamp;
        try {
            timestamp = Parser.matchGroup1(regexPattern, getOriginalUrl());
        } catch (final Parser.RegexException missingTimestamp) {
            return -2;
        }
        if (timestamp.isEmpty()) return 0;

        try {
            String seconds = "";
            String minutes = "";
            String hours = "";
            try {
                seconds = Parser.matchGroup1("(\\d+)s", timestamp);
                minutes = Parser.matchGroup1("(\\d+)m", timestamp);
                hours = Parser.matchGroup1("(\\d+)h", timestamp);
            } catch (final Parser.RegexException labelledTimestamp) {
                if (seconds.isEmpty() && minutes.isEmpty()) {
                    seconds = Parser.matchGroup1("t=(\\d+)", timestamp);
                }
            }
            return (seconds.isEmpty() ? 0 : Long.parseLong(seconds))
                    + 60L * (minutes.isEmpty() ? 0 : Long.parseLong(minutes))
                    + 3600L * (hours.isEmpty() ? 0 : Long.parseLong(hours));
        } catch (final NumberFormatException | ParsingException error) {
            throw new ParsingException("Could not get timestamp.", error);
        }
    }

    @Nonnull public String getHost() throws ParsingException { return ""; }
    public Privacy getPrivacy() throws ParsingException { return Privacy.PUBLIC; }
    @Nonnull public String getCategory() throws ParsingException { return ""; }
    @Nonnull public String getLicence() throws ParsingException { return ""; }
    @Nullable public Locale getLanguageInfo() throws ParsingException { return null; }
    @Nonnull public List<String> getTags() throws ParsingException { return List.of(); }
    @Nonnull public String getSupportInfo() throws ParsingException { return ""; }
    @Nonnull public List<StreamSegment> getStreamSegments() throws ParsingException {
        return List.of();
    }
    @Nonnull public List<MetaInfo> getMetaInfo() throws ParsingException { return List.of(); }
    public boolean isShortFormContent() throws ParsingException { return false; }
    @Nonnull public ContentAvailability getContentAvailability() throws ParsingException {
        return ContentAvailability.UNKNOWN;
    }

    public enum Privacy { PUBLIC, UNLISTED, PRIVATE, INTERNAL, OTHER }
}
