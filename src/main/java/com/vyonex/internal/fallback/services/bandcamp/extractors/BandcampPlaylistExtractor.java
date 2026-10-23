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
    private Document document;
    private JsonObject album;
    private JsonArray tracks;
    private String name;

    public BandcampPlaylistExtractor(final StreamingService service,
                                     final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String html = downloader.get(getLinkHandler().getUrl()).responseBody();
        document = Jsoup.parse(html);
        album = BandcampStreamExtractor.getAlbumInfoJson(html);
        tracks = album.getArray("trackinfo");
        name = BandcampExtractorHelper.getJsonAttribute(html, "data-embed")
                .getString("album_title");
        if (name == null || name.isBlank()) {
            throw new ParsingException("Bandcamp album response is missing its title");
        }
        if (tracks.isEmpty()) throw new PaidContentException("This album requires purchase");
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageId(album.getLong("art_id"), true);
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        try {
            final URI uri = new URI(album.getString("url"));
            if (uri.getHost() == null) throw new URISyntaxException(uri.toString(), "Missing host");
            return uri.getScheme() + "://" + uri.getRawAuthority() + "/";
        } catch (final URISyntaxException | NullPointerException e) {
            throw new ParsingException("Bandcamp album response has an invalid URL", e);
        }
    }

    @Override
    public String getUploaderName() {
        return album.getString("artist", "");
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        final Element image = document.selectFirst(".band-photo[src]");
        return BandcampExtractorHelper.getImagesFromImageUrl(image == null ? "" : image.attr("src"));
    }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Override
    public long getStreamCount() { return tracks.size(); }

    @Nonnull
    @Override
    public Description getDescription() {
        final List<String> sections = new ArrayList<>();
        for (final String selector : List.of("#trackInfo .tralbum-about",
                "#trackInfo .tralbum-credits", "#license")) {
            final Element section = document.selectFirst(selector);
            if (section != null && !section.html().isBlank()) sections.add(section.html());
        }
        return sections.isEmpty() ? Description.EMPTY_DESCRIPTION
                : new Description(String.join("", sections), Description.HTML);
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws ExtractionException {
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        for (final Object value : tracks) {
            if (!(value instanceof JsonObject track)) continue;
            if (tracks.size() < INDIVIDUAL_COVER_LIMIT) {
                items.commit(new BandcampPlaylistStreamInfoItemExtractor(
                        track, getUploaderUrl(), getService()));
            } else {
                items.commit(new BandcampPlaylistStreamInfoItemExtractor(
                        track, getUploaderUrl(), getThumbnails()));
            }
        }
        return new InfoItemsPage<>(items, null);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page) { return null; }

    @Nonnull
    @Override
    public String getName() { return name; }
}
