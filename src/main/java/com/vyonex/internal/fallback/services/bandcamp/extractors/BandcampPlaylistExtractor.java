// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.PaidContentException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.playlist.PlaylistExtractor;
import com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem.BandcampPlaylistStreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class BandcampPlaylistExtractor extends PlaylistExtractor {
    // shortcut: per-track covers stop at ten tracks; remove when Bandcamp exposes art IDs in this page.
    private static final int INDIVIDUAL_COVER_LIMIT = 10;
    private String albumUrl;
    private String artist;
    private String name;
    private long albumArtId;
    private int trackCount;
    private List<Image> uploaderAvatars = List.of();
    private Description description;
    private List<TrackData> tracks = List.of();

    public BandcampPlaylistExtractor(final StreamingService service,
                                     final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String html = downloader.get(getLinkHandler().getUrl()).responseBody();
        final Document document = Jsoup.parse(html);
        final JsonObject album = BandcampStreamExtractor.getAlbumInfoJson(html);
        final JsonArray trackInfo = album.getArray("trackinfo");
        trackCount = trackInfo.size();
        name = BandcampExtractorHelper.getJsonAttribute(html, "data-embed")
                .getString("album_title");
        if (name == null || name.isBlank()) {
            throw new ParsingException("Bandcamp album response is missing its title");
        }
        if (trackInfo.isEmpty()) throw new PaidContentException("This album requires purchase");
        albumUrl = album.getString("url");
        artist = album.getString("artist", "");
        albumArtId = album.getLong("art_id");
        final List<TrackData> mappedTracks = new ArrayList<>();
        for (final Object value : trackInfo) {
            if (!(value instanceof JsonObject track)) continue;
            mappedTracks.add(new TrackData(track.getString("title"),
                    track.getString("title_link"), (long) track.getDouble("duration"),
                    track.getString("artist", "")));
        }
        tracks = List.copyOf(mappedTracks);
        final Element image = document.selectFirst(".band-photo[src]");
        uploaderAvatars = BandcampExtractorHelper.getImagesFromImageUrl(
                image == null ? "" : image.attr("src"));
        final List<String> sections = new ArrayList<>();
        for (final String selector : List.of("#trackInfo .tralbum-about",
                "#trackInfo .tralbum-credits", "#license")) {
            final Element section = document.selectFirst(selector);
            if (section != null && !section.html().isBlank()) sections.add(section.html());
        }
        description = sections.isEmpty() ? Description.EMPTY_DESCRIPTION
                : new Description(String.join("", sections), Description.HTML);
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageId(albumArtId, true);
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        try {
            final URI uri = new URI(albumUrl);
            if (uri.getHost() == null) throw new URISyntaxException(uri.toString(), "Missing host");
            return uri.getScheme() + "://" + uri.getRawAuthority() + "/";
        } catch (final URISyntaxException | NullPointerException e) {
            throw new ParsingException("Bandcamp album response has an invalid URL", e);
        }
    }

    @Override
    public String getUploaderName() {
        return artist;
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        return uploaderAvatars;
    }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Override
    public long getStreamCount() { return trackCount; }

    @Nonnull
    @Override
    public Description getDescription() {
        return description;
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws ExtractionException {
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        String uploaderUrl = null;
        for (final TrackData track : tracks) {
            if (uploaderUrl == null) uploaderUrl = getUploaderUrl();
            if (trackCount < INDIVIDUAL_COVER_LIMIT) {
                items.commit(new BandcampPlaylistStreamInfoItemExtractor(track.title(),
                        track.titleLink(), track.duration(), track.artist(), uploaderUrl,
                        getService(), List.of()));
            } else {
                items.commit(new BandcampPlaylistStreamInfoItemExtractor(track.title(),
                        track.titleLink(), track.duration(), track.artist(), uploaderUrl,
                        null, getThumbnails()));
            }
        }
        return new InfoItemsPage<>(items, null);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page) { return null; }

    @Nonnull
    @Override
    public String getName() { return name; }

    private record TrackData(String title, String titleLink, long duration, String artist) { }
}
