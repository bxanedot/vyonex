// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ContentNotSupportedException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.stream.AudioStream;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.stream.StreamSegment;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.stream.VideoStream;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.BASE_API_URL;
import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.BASE_URL;

public class BandcampRadioStreamExtractor extends StreamExtractor {
    private static final String API_URL = BASE_API_URL + "/bcweekly/1/get?id=";
    private static final String HOST_AVATAR = BASE_URL
            + "/img/buttons/bandcamp-button-circle-whitecolor-512.png";

    private RadioShow show;

    public BandcampRadioStreamExtractor(final StreamingService service,
                                        final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    static JsonObject query(final Downloader downloader, final int id) throws ParsingException {
        try {
            final Response response = downloader.get(API_URL + id);
            if (response == null || response.responseBody() == null
                    || response.responseBody().isBlank() || response.responseCode() >= 400) {
                throw new ParsingException("Bandcamp returned no radio-show data");
            }
            return JsonParser.object().from(response.responseBody());
        } catch (final IOException | ReCaptchaException | JsonParserException error) {
            throw new ParsingException("Could not load Bandcamp radio show", error);
        }
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) throws ParsingException {
        final int id;
        try {
            id = Integer.parseInt(getId());
        } catch (final NumberFormatException error) {
            throw new ParsingException("Invalid Bandcamp radio show ID", error);
        }
        show = readShow(query(downloader, id));
    }

    private static RadioShow readShow(final JsonObject data) {
        final List<SegmentData> segments = new ArrayList<>();
        for (final Object value : data.getArray("tracks")) {
            if (!(value instanceof JsonObject track)) continue;
            final String title = track.getString("title");
            if (title == null || title.isBlank()) continue;
            segments.add(new SegmentData(title, track.getInt("timecode"),
                    track.getLong("track_art_id"), track.getString("artist")));
        }

        final JsonObject audio = data.getObject("audio_stream");
        return new RadioShow(data.getString("subtitle"), data.getString("image_caption"),
                data.getString("published_date"), data.getLong("show_image_id"),
                data.getString("desc"), data.getLong("audio_duration"),
                audio.getString("mp3-128"), audio.getString("opus-lo"), List.copyOf(segments));
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return required(show().title(), "subtitle");
    }

    @Nonnull
    @Override
    public String getUploaderUrl() throws ContentNotSupportedException {
        throw new ContentNotSupportedException("Bandcamp radio has no uploader page");
    }

    @Nonnull
    @Override
    public String getUrl() { return getLinkHandler().getUrl(); }

    @Nonnull
    @Override
    public String getUploaderName() throws ParsingException {
        final String caption = show().imageCaption();
        final Element link = Jsoup.parse(caption == null ? "" : caption).selectFirst("a");
        if (link == null || link.text().isBlank()) {
            throw new ParsingException("Bandcamp radio response has no host name");
        }
        return link.text();
    }

    @Nullable
    @Override
    public String getTextualUploadDate() { return show().publishedDate(); }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        final String date = show().publishedDate();
        if (date == null || date.isBlank()) return null;
        try {
            return new DateWrapper(LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant());
        } catch (final DateTimeParseException error) {
            throw new ParsingException("Invalid Bandcamp radio publication date", error);
        }
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageId(show().imageId(), false);
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        return List.of(new Image(HOST_AVATAR, 512, 512, Image.ResolutionLevel.MEDIUM));
    }

    @Nonnull
    @Override
    public Description getDescription() {
        return new Description(show().description(), Description.PLAIN_TEXT);
    }

    @Override
    public long getLength() { return show().duration(); }

    @Override
    public List<AudioStream> getAudioStreams() {
        final List<AudioStream> streams = new ArrayList<>(2);
        addAudio(streams, show().mp3Url(), "mp3-128", MediaFormat.MP3, 128);
        addAudio(streams, show().opusUrl(), "opus-lo", MediaFormat.OPUS, 100);
        return streams;
    }

    @Nonnull
    @Override
    public List<StreamSegment> getStreamSegments() {
        final List<StreamSegment> segments = new ArrayList<>(show().segments().size());
        for (final SegmentData data : show().segments()) {
            final StreamSegment segment = new StreamSegment(data.title(), data.timecode());
            segment.setPreviewUrl(BandcampExtractorHelper.getImageUrl(data.artId(), true));
            segment.setChannelName(data.artist());
            segments.add(segment);
        }
        return segments;
    }

    @Override public List<VideoStream> getVideoStreams() { return List.of(); }
    @Override public List<VideoStream> getVideoOnlyStreams() { return List.of(); }
    @Override public StreamType getStreamType() { return StreamType.AUDIO_STREAM; }

    private RadioShow show() {
        if (show == null) throw new IllegalStateException("Bandcamp radio page has not been fetched");
        return show;
    }

    private static String required(final String value, final String field) throws ParsingException {
        if (value == null || value.isBlank()) {
            throw new ParsingException("Bandcamp radio response is missing " + field);
        }
        return value;
    }

    private static void addAudio(final List<AudioStream> streams, final String url,
                                 final String id, final MediaFormat format, final int bitrate) {
        if (url == null || url.isBlank()) return;
        streams.add(new AudioStream.Builder().setId(id).setContent(url, true)
                .setMediaFormat(format).setAverageBitrate(bitrate).build());
    }

    private record RadioShow(String title, String imageCaption, String publishedDate,
                             long imageId, String description, long duration, String mp3Url,
                             String opusUrl, List<SegmentData> segments) { }

    private record SegmentData(String title, int timecode, long artId, String artist) { }
}
