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
import com.vyonex.internal.json.JsonObject;

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
    @Nullable private Event event;
    @Nullable private String conferenceLogo;

    public MediaCCCStreamExtractor(final StreamingService service, final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Nullable @Override public String getTextualUploadDate() { return event.releaseDate(); }
    @Nullable @Override public DateWrapper getUploadDate() throws ParsingException {
        return DateWrapper.fromInstant(getTextualUploadDate());
    }
    @Nonnull @Override public List<Image> getThumbnails() { return event.thumbnails(); }
    @Nonnull @Override public Description getDescription() {
        return new Description(event.description(), Description.PLAIN_TEXT);
    }
    @Override public long getLength() { return event.length(); }
    @Override public long getViewCount() { return event.viewCount(); }
    @Nonnull @Override public String getUploaderUrl() throws ParsingException {
        return MediaCCCConferenceLinkHandlerFactory.getInstance()
                .getUrl(event.conferenceId(), List.of(), "");
    }
    @Nonnull @Override public String getUploaderName() throws ParsingException {
        return event.conferenceId();
    }
    @Override public List<Image> getUploaderAvatars() {
        return getImageListFromLogoImageUrl(conferenceLogo);
    }

    @Override public List<AudioStream> getAudioStreams() throws ExtractionException {
        final List<AudioStream> result = new ArrayList<>();
        for (final Recording recording : event.recordings()) {
            if (!recording.mimeType().startsWith("audio")) continue;
            final AudioStream.Builder stream = new AudioStream.Builder()
                    .setId(recording.fileName())
                    .setContent(recording.url(), true)
                    .setMediaFormat(audioFormat(recording.mimeType()))
                    .setAverageBitrate(UNKNOWN_BITRATE);
            final String language = recording.language();
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
        for (final Recording recording : event.recordings()) {
            if (!recording.mimeType().startsWith("video")) continue;
            final String mimeType = recording.mimeType();
            final MediaFormat format = mimeType.endsWith("webm") ? MediaFormat.WEBM
                    : mimeType.endsWith("mp4") ? MediaFormat.MPEG_4 : null;
            result.add(new VideoStream.Builder()
                    .setId(recording.fileName())
                    .setContent(recording.url(), true)
                    .setIsVideoOnly(false)
                    .setMediaFormat(format)
                    .setResolution(recording.height() + "p")
                    .build());
        }
        return result;
    }

    @Override public List<VideoStream> getVideoOnlyStreams() { return List.of(); }
    @Override public StreamType getStreamType() { return StreamType.VIDEO_STREAM; }

    @Override public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String eventUrl = MediaCCCStreamLinkHandlerFactory.VIDEO_API_ENDPOINT + getId();
        event = readEvent(downloader, eventUrl);
        conferenceLogo = readConferenceLogo(downloader, event.conferenceId());
    }

    private static Event readEvent(final Downloader downloader, final String url)
            throws IOException, ExtractionException {
        final JsonObject object = MediaCCCParsingHelper.fetchJsonObject(downloader, url, null);
        final String conferenceReference = object.getString("conference_url");
        if (conferenceReference == null || conferenceReference.isBlank()) {
            throw new ExtractionException("The event response has no conference URL.");
        }
        final String conferenceId = MediaCCCConferenceLinkHandlerFactory.getInstance()
                .getId(conferenceReference);
        final List<String> tags = new ArrayList<>();
        for (final Object tag : object.getArray("tags")) {
            if (tag instanceof String) tags.add((String) tag);
        }
        final List<Recording> recordings = new ArrayList<>();
        for (final JsonObject item : object.getArray("recordings").streamAsJsonObjects().toList()) {
            recordings.add(new Recording(item.getString("filename", ID_UNKNOWN),
                    item.getString("recording_url"), item.getString("mime_type", ""),
                    item.getString("language"), item.getInt("height")));
        }
        return new Event(object.getString("title"), object.getString("release_date"),
                object.getString("description"), object.getLong("length"),
                object.getLong("view_count"), conferenceId, object.getString("frontend_link"),
                object.getString("original_language"), List.copyOf(tags),
                List.copyOf(recordings), getThumbnailsFromStreamItem(object));
    }

    private static String readConferenceLogo(final Downloader downloader, final String conferenceId)
            throws IOException, ExtractionException {
        final String url = MediaCCCConferenceLinkHandlerFactory.CONFERENCE_API_ENDPOINT
                + conferenceId;
        return MediaCCCParsingHelper.fetchJsonObject(downloader, url, null).getString("logo_url");
    }

    @Nonnull @Override public String getName() throws ParsingException { return event.title(); }
    @Nonnull @Override public String getOriginalUrl() { return event.frontendLink(); }
    @Override public Locale getLanguageInfo() throws ParsingException {
        return Localization.getLocaleFromThreeLetterCode(event.originalLanguage());
    }
    @Nonnull @Override public List<String> getTags() { return event.tags(); }

    private record Event(@Nullable String title, @Nullable String releaseDate,
                         @Nullable String description, long length, long viewCount,
                         String conferenceId, @Nullable String frontendLink,
                         @Nullable String originalLanguage, List<String> tags,
                         List<Recording> recordings, List<Image> thumbnails) { }

    private record Recording(String fileName, @Nullable String url, String mimeType,
                             @Nullable String language, int height) { }
}
