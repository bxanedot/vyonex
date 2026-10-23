// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.AudioStream;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.stream.VideoStream;
import com.vyonex.internal.fallback.utils.LocaleCompat;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.vyonex.internal.fallback.stream.AudioStream.UNKNOWN_BITRATE;
import static com.vyonex.internal.fallback.stream.Stream.ID_UNKNOWN;
import static com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper.getImageListFromLogoImageUrl;
import static com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper.getThumbnailsFromStreamItem;

public class MediaCCCStreamExtractor extends StreamExtractor {
    @Nullable private JsonObject event;
    @Nullable private JsonObject conference;

    public MediaCCCStreamExtractor(final StreamingService service, final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Nullable @Override public String getTextualUploadDate() {
        return event.getString("release_date");
    }
    @Nullable @Override public DateWrapper getUploadDate() throws ParsingException {
        return DateWrapper.fromOffsetDateTime(getTextualUploadDate());
    }
    @Nonnull @Override public List<Image> getThumbnails() {
        return getThumbnailsFromStreamItem(event);
    }
    @Nonnull @Override public Description getDescription() {
        return new Description(event.getString("description"), Description.PLAIN_TEXT);
    }
    @Override public long getLength() { return event.getInt("length"); }
    @Override public long getViewCount() { return event.getInt("view_count"); }
    @Nonnull @Override public String getUploaderUrl() throws ParsingException {
        return MediaCCCConferenceLinkHandlerFactory.CONFERENCE_PATH + getUploaderName();
    }
    @Nonnull @Override public String getUploaderName() throws ParsingException {
        return MediaCCCConferenceLinkHandlerFactory.getInstance()
                .getId(event.getString("conference_url"));
    }
    @Override public List<Image> getUploaderAvatars() {
        return getImageListFromLogoImageUrl(conference.getString("logo_url"));
    }

    @Override public List<AudioStream> getAudioStreams() throws ExtractionException {
        final List<AudioStream> result = new ArrayList<>();
        for (final JsonObject recording : event.getArray("recordings").streamAsJsonObjects()
                .toList()) {
            final String mimeType = recording.getString("mime_type", "");
            if (!mimeType.startsWith("audio")) continue;
            final AudioStream.Builder stream = new AudioStream.Builder()
                    .setId(recording.getString("filename", ID_UNKNOWN))
                    .setContent(recording.getString("recording_url"), true)
                    .setMediaFormat(audioFormat(mimeType))
                    .setAverageBitrate(UNKNOWN_BITRATE);
            final String language = recording.getString("language");
            if (language != null && !language.contains("-")) {
                stream.setAudioLocale(LocaleCompat.forLanguageTag(language).orElseThrow(() ->
                        new ParsingException("Cannot convert language to a locale: " + language)));
            }
            result.add(stream.build());
        }
        return result;
    }

    private static MediaFormat audioFormat(final String mimeType) {
        if (mimeType.endsWith("opus")) return MediaFormat.OPUS;
        if (mimeType.endsWith("mpeg")) return MediaFormat.MP3;
        if (mimeType.endsWith("ogg")) return MediaFormat.OGG;
        return null;
    }

    @Override public List<VideoStream> getVideoStreams() {
        final List<VideoStream> result = new ArrayList<>();
        final JsonArray recordings = event.getArray("recordings");
        for (final JsonObject recording : recordings.streamAsJsonObjects().toList()) {
            final String mimeType = recording.getString("mime_type", "");
            if (!mimeType.startsWith("video")) continue;
            final MediaFormat format = mimeType.endsWith("webm") ? MediaFormat.WEBM
                    : mimeType.endsWith("mp4") ? MediaFormat.MPEG_4 : null;
            result.add(new VideoStream.Builder()
                    .setId(recording.getString("filename", ID_UNKNOWN))
                    .setContent(recording.getString("recording_url"), true)
                    .setIsVideoOnly(false)
                    .setMediaFormat(format)
                    .setResolution(recording.getInt("height") + "p")
                    .build());
        }
        return result;
    }

    @Override public List<VideoStream> getVideoOnlyStreams() { return List.of(); }
    @Override public StreamType getStreamType() { return StreamType.VIDEO_STREAM; }

    @Override public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String eventUrl = MediaCCCStreamLinkHandlerFactory.VIDEO_API_ENDPOINT + getId();
        try {
            event = JsonParser.object().from(downloader.get(eventUrl).responseBody());
        } catch (final JsonParserException error) {
            throw new ExtractionException("Could not parse event response from " + eventUrl, error);
        }

        final String conferenceUrl = event.getString("conference_url");
        if (conferenceUrl == null || conferenceUrl.isBlank()) {
            throw new ExtractionException("The event response has no conference URL.");
        }
        try {
            conference = JsonParser.object().from(downloader.get(conferenceUrl).responseBody());
        } catch (final JsonParserException error) {
            throw new ExtractionException("Could not parse conference response from "
                    + conferenceUrl, error);
        }
    }

    @Nonnull @Override public String getName() throws ParsingException {
        return event.getString("title");
    }
    @Nonnull @Override public String getOriginalUrl() {
        return event.getString("frontend_link");
    }
    @Override public Locale getLanguageInfo() throws ParsingException {
        return Localization.getLocaleFromThreeLetterCode(event.getString("original_language"));
    }
    @Nonnull @Override public List<String> getTags() {
        final List<String> tags = new ArrayList<>();
        for (final Object tag : event.getArray("tags")) {
            if (tag instanceof String) tags.add((String) tag);
        }
        return List.copyOf(tags);
    }
}
