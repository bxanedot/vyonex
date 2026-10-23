// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ContentNotSupportedException;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemsCollector;
import com.vyonex.internal.fallback.stream.AudioStream;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamSegment;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.BASE_API_URL;
import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.BASE_URL;

public class BandcampRadioStreamExtractor extends BandcampStreamExtractor {
    private JsonObject show;

    public BandcampRadioStreamExtractor(final StreamingService service,
                                        final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    static JsonObject query(final Downloader downloader, final int id) throws ParsingException {
        try {
            return JsonParser.object().from(downloader.get(
                    BASE_API_URL + "/bcweekly/1/get?id=" + id).responseBody());
        } catch (final IOException | ReCaptchaException | JsonParserException e) {
            throw new ParsingException("Could not load Bandcamp radio show", e);
        }
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) throws ExtractionException {
        try {
            show = query(downloader, Integer.parseInt(getId()));
        } catch (final NumberFormatException e) {
            throw new ParsingException("Invalid Bandcamp radio show ID", e);
        }
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return required("subtitle");
    }

    @Nonnull
    @Override
    public String getUploaderUrl() throws ContentNotSupportedException {
        throw new ContentNotSupportedException("Bandcamp radio has no uploader page");
    }

    @Nonnull
    @Override
    public String getUrl() {
        return getLinkHandler().getUrl();
    }

    @Nonnull
    @Override
    public String getUploaderName() throws ParsingException {
        final Element name = Jsoup.parse(show.getString("image_caption", ""))
                .selectFirst("a");
        if (name == null || name.text().isBlank()) {
            throw new ParsingException("Bandcamp radio response has no host name");
        }
        return name.text();
    }

    @Nullable
    @Override
    public String getTextualUploadDate() {
        return show.getString("published_date");
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageId(show.getLong("show_image_id"), false);
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        return List.of(new Image(BASE_URL + "/img/buttons/bandcamp-button-circle-whitecolor-512.png",
                512, 512, Image.ResolutionLevel.MEDIUM));
    }

    @Nonnull
    @Override
    public Description getDescription() {
        return new Description(show.getString("desc"), Description.PLAIN_TEXT);
    }

    @Override
    public long getLength() {
        return show.getLong("audio_duration");
    }

    @Override
    public List<AudioStream> getAudioStreams() {
        final JsonObject sources = show.getObject("audio_stream");
        final List<AudioStream> streams = new ArrayList<>(2);
        addAudio(streams, sources, "mp3-128", MediaFormat.MP3, 128);
        addAudio(streams, sources, "opus-lo", MediaFormat.OPUS, 100);
        return streams;
    }

    @Nonnull
    @Override
    public List<StreamSegment> getStreamSegments() {
        final JsonArray tracks = show.getArray("tracks");
        final List<StreamSegment> segments = new ArrayList<>(tracks.size());
        for (final Object value : tracks) {
            if (!(value instanceof JsonObject track)) continue;
            final String title = track.getString("title");
            if (title == null) continue;
            final StreamSegment segment = new StreamSegment(title, track.getInt("timecode"));
            segment.setPreviewUrl(BandcampExtractorHelper.getImageUrl(
                    track.getLong("track_art_id"), true));
            segment.setChannelName(track.getString("artist"));
            segments.add(segment);
        }
        return segments;
    }

    @Nonnull
    @Override
    public String getLicence() { return ""; }

    @Nonnull
    @Override
    public String getCategory() { return ""; }

    @Nonnull
    @Override
    public List<String> getTags() { return List.of(); }

    @Override
    public PlaylistInfoItemsCollector getRelatedItems() { return null; }

    private String required(final String key) throws ParsingException {
        final String value = show.getString(key);
        if (value == null || value.isBlank()) {
            throw new ParsingException("Bandcamp radio response is missing " + key);
        }
        return value;
    }

    private static void addAudio(final List<AudioStream> streams, final JsonObject sources,
                                 final String id, final MediaFormat format, final int bitrate) {
        final String url = sources.getString(id);
        if (url != null && !url.isBlank()) {
            streams.add(new AudioStream.Builder().setId(id).setContent(url, true)
                    .setMediaFormat(format).setAverageBitrate(bitrate).build());
        }
    }
}
