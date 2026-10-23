// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.PaidContentException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemsCollector;
import com.vyonex.internal.fallback.stream.AudioStream;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.stream.VideoStream;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BandcampStreamExtractor extends StreamExtractor {
    private JsonObject pageData;
    private JsonObject current;
    private JsonObject track;
    private Document document;

    public BandcampStreamExtractor(final StreamingService service, final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String html = downloader.get(getLinkHandler().getUrl()).responseBody();
        document = Jsoup.parse(html);
        pageData = getAlbumInfoJson(html);
        current = pageData.getObject("current");
        final JsonArray tracks = pageData.getArray("trackinfo");
        if (tracks.isEmpty()) throw new ParsingException("Bandcamp page contains no track data");
        if (tracks.size() != 1) {
            throw new ExtractionException("The URL points to an album, not a single track");
        }
        track = tracks.getObject(0);
        if (track.get("file") == null) {
            throw new PaidContentException("This track requires purchase before playback");
        }
    }

    public static JsonObject getAlbumInfoJson(final String html) throws ParsingException {
        return BandcampExtractorHelper.getJsonAttribute(html, "data-tralbum");
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return requiredString(current, "title");
    }

    @Nonnull
    @Override
    public String getUploaderUrl() throws ParsingException {
        try {
            final URI uri = new URI(getUrl());
            return uri.getScheme() + "://" + uri.getRawAuthority() + "/";
        } catch (final URISyntaxException e) {
            throw new ParsingException("Bandcamp returned an invalid track URL", e);
        }
    }

    @Nonnull
    @Override
    public String getUrl() throws ParsingException {
        return https(requiredString(pageData, "url"));
    }

    @Nonnull
    @Override
    public String getUploaderName() throws ParsingException {
        return requiredString(pageData, "artist");
    }

    @Nullable
    @Override
    public String getTextualUploadDate() {
        return current.getString("publish_date");
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        final String published = getTextualUploadDate();
        return published == null || published.isBlank()
                ? null : BandcampExtractorHelper.parseDate(published);
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        return BandcampExtractorHelper.getImagesFromImageId(pageData.getLong("art_id"), true);
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        final Element image = document.selectFirst(".band-photo[src]");
        return BandcampExtractorHelper.getImagesFromImageUrl(image == null ? "" : image.attr("src"));
    }

    @Nonnull
    @Override
    public Description getDescription() {
        final String text = List.of("about", "lyrics", "credits").stream()
                .map(current::getString)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("\n\n"));
        return new Description(text, Description.PLAIN_TEXT);
    }

    @Override
    public List<AudioStream> getAudioStreams() {
        final String audio = track.getObject("file").getString("mp3-128");
        if (audio == null || audio.isBlank()) return List.of();
        return List.of(new AudioStream.Builder().setId("mp3-128").setContent(audio, true)
                .setMediaFormat(MediaFormat.MP3).setAverageBitrate(128).build());
    }

    @Override
    public long getLength() {
        return (long) track.getDouble("duration");
    }

    @Override
    public List<VideoStream> getVideoStreams() { return List.of(); }

    @Override
    public List<VideoStream> getVideoOnlyStreams() { return List.of(); }

    @Override
    public StreamType getStreamType() { return StreamType.AUDIO_STREAM; }

    @Override
    public PlaylistInfoItemsCollector getRelatedItems() {
        final PlaylistInfoItemsCollector collector = new PlaylistInfoItemsCollector(getServiceId());
        document.select(".recommended-album").stream()
                .map(BandcampRelatedPlaylistInfoItemExtractor::new).forEach(collector::commit);
        return collector;
    }

    @Nonnull
    @Override
    public String getCategory() {
        final Element tag = document.selectFirst(".tralbum-tags .tag");
        return tag == null ? "" : tag.text();
    }

    @Nonnull
    @Override
    public String getLicence() {
        final Element license = document.selectFirst("#license");
        return license == null ? "Unknown" : license.text();
    }

    @Nonnull
    @Override
    public List<String> getTags() {
        return document.select("[itemprop=keywords]").stream().map(Element::text)
                .filter(value -> !value.isBlank()).collect(Collectors.toCollection(ArrayList::new));
    }

    private static String requiredString(final JsonObject object, final String key)
            throws ParsingException {
        final String value = object.getString(key);
        if (value == null || value.isBlank()) {
            throw new ParsingException("Bandcamp response is missing " + key);
        }
        return value;
    }

    private static String https(final String value) {
        return value.startsWith("http://") ? "https://" + value.substring(7) : value;
    }
}
