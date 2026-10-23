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

public class BandcampStreamExtractor extends StreamExtractor {
    private TrackPage page;

    public BandcampStreamExtractor(final StreamingService service, final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String html = downloader.get(getLinkHandler().getUrl()).responseBody();
        final JsonObject album = getAlbumInfoJson(html);
        final JsonObject current = album.getObject("current");
        final JsonArray tracks = album.getArray("trackinfo");
        if (tracks == null || tracks.isEmpty()) {
            throw new ParsingException("Bandcamp page contains no track data");
        }
        if (tracks.size() != 1) {
            throw new ExtractionException("The URL points to an album, not a single track");
        }

        final JsonObject track = tracks.getObject(0);
        if (track.get("file") == null) {
            throw new PaidContentException("This track requires purchase before playback");
        }
        page = new TrackPage(album, current, track, Jsoup.parse(html));
    }

    public static JsonObject getAlbumInfoJson(final String html) throws ParsingException {
        return BandcampExtractorHelper.getJsonAttribute(html, "data-tralbum");
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return requiredString(data().current(), "title");
    }

    @Nonnull
    @Override
    public String getUploaderUrl() throws ParsingException {
        try {
            final URI uri = new URI(getUrl());
            if (uri.getScheme() == null || uri.getRawAuthority() == null) {
                throw new URISyntaxException(getUrl(), "Track URL has no origin");
            }
            return uri.getScheme() + "://" + uri.getRawAuthority() + "/";
        } catch (final URISyntaxException e) {
            throw new ParsingException("Bandcamp returned an invalid track URL", e);
        }
    }

    @Nonnull
    @Override
    public String getUrl() throws ParsingException {
        return https(requiredString(data().album(), "url"));
    }

    @Nonnull
    @Override
    public String getUploaderName() throws ParsingException {
        return requiredString(data().album(), "artist");
    }

    @Nullable
    @Override
    public String getTextualUploadDate() {
        return data().current().getString("publish_date");
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        final String date = getTextualUploadDate();
        return date == null || date.isBlank() ? null : BandcampExtractorHelper.parseDate(date);
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        return BandcampExtractorHelper.getImagesFromImageId(data().album().getLong("art_id"), true);
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        final Element image = data().document().selectFirst(".band-photo[src]");
        return BandcampExtractorHelper.getImagesFromImageUrl(
                image == null ? "" : image.attr("src"));
    }

    @Nonnull
    @Override
    public Description getDescription() {
        final List<String> sections = new ArrayList<>(3);
        for (final String key : List.of("about", "lyrics", "credits")) {
            final String value = data().current().getString(key);
            if (value != null && !value.isBlank()) sections.add(value);
        }
        return new Description(String.join("\n\n", sections), Description.PLAIN_TEXT);
    }

    @Override
    public List<AudioStream> getAudioStreams() {
        final JsonObject files = data().track().getObject("file");
        final String url = files == null ? null : files.getString("mp3-128");
        if (url == null || url.isBlank()) return List.of();
        return List.of(new AudioStream.Builder().setId("mp3-128").setContent(url, true)
                .setMediaFormat(MediaFormat.MP3).setAverageBitrate(128).build());
    }

    @Override
    public long getLength() {
        return (long) data().track().getDouble("duration");
    }

    @Override public List<VideoStream> getVideoStreams() { return List.of(); }
    @Override public List<VideoStream> getVideoOnlyStreams() { return List.of(); }
    @Override public StreamType getStreamType() { return StreamType.AUDIO_STREAM; }

    @Override
    public PlaylistInfoItemsCollector getRelatedItems() {
        final PlaylistInfoItemsCollector items = new PlaylistInfoItemsCollector(getServiceId());
        for (final Element album : data().document().select(".recommended-album")) {
            items.commit(new BandcampRelatedPlaylistInfoItemExtractor(album));
        }
        return items;
    }

    @Nonnull
    @Override
    public String getCategory() {
        final Element tag = data().document().selectFirst(".tralbum-tags .tag");
        return tag == null ? "" : tag.text();
    }

    @Nonnull
    @Override
    public String getLicence() {
        final Element license = data().document().selectFirst("#license");
        return license == null ? "Unknown" : license.text();
    }

    @Nonnull
    @Override
    public List<String> getTags() {
        final List<String> tags = new ArrayList<>();
        for (final Element keyword : data().document().select("[itemprop=keywords]")) {
            final String value = keyword.text();
            if (!value.isBlank()) tags.add(value);
        }
        return tags;
    }

    private TrackPage data() {
        if (page == null) throw new IllegalStateException("Track page has not been fetched");
        return page;
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

    private record TrackPage(JsonObject album, JsonObject current, JsonObject track,
                             Document document) { }
}
