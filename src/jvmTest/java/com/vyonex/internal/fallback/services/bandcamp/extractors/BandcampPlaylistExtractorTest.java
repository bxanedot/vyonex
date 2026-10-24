// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.PaidContentException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.bandcamp.BandcampService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BandcampPlaylistExtractorTest {
    @Test
    void extractsAlbumAndTrackItemsFromOfflinePages() throws Exception {
        final String album = "<div data-tralbum='{" +
                "\"url\":\"https://artist.bandcamp.com/album/record\","
                + "\"artist\":\"Artist\",\"art_id\":50,\"trackinfo\":[{"
                + "\"title\":\"Song\",\"title_link\":\"/track/song\","
                + "\"duration\":123}]}'></div>"
                + "<div data-embed='{\"album_title\":\"Record\"}'></div>"
                + "<div id='trackInfo'><div class='tralbum-about'><b>Album notes</b></div></div>";
        final String track = "<div data-tralbum='{" +
                "\"url\":\"https://artist.bandcamp.com/track/song\","
                + "\"artist\":\"Artist\",\"art_id\":77,\"trackinfo\":[{"
                + "\"file\":{\"mp3-128\":\"https://audio.example/song.mp3\"},"
                + "\"duration\":123}],\"current\":{\"title\":\"Song\"}}'></div>";
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override
            public Response execute(final Request request) {
                final String body = request.url().endsWith("/album/record") ? album : track;
                return new Response(200, "OK", Map.of(), body, request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final BandcampService service = new BandcampService(1);
            final String url = "https://artist.bandcamp.com/album/record";
            final BandcampPlaylistExtractor extractor = new BandcampPlaylistExtractor(service,
                    new ListLinkHandler(url, url, url, List.of(), ""));
            extractor.onFetchPage(offline);

            assertEquals("Record", extractor.getName());
            assertEquals("Artist", extractor.getUploaderName());
            assertEquals("https://artist.bandcamp.com/", extractor.getUploaderUrl());
            assertEquals(1, extractor.getStreamCount());
            assertEquals("<b>Album notes</b>", extractor.getDescription().getContent());
            final var items = extractor.getInitialPage().getItems();
            assertEquals("Song", items.get(0).getName());
            assertEquals("https://artist.bandcamp.com/track/song",
                    items.get(0).getUrl());
            assertEquals("https://f4.bcbits.com/img/a77_10.jpg",
                    items.get(0).getThumbnails().get(0).getUrl());
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void marksAlbumsWithoutTrackDataAsPaid() {
        final String album = "<div data-tralbum='{" +
                "\"url\":\"https://artist.bandcamp.com/album/record\","
                + "\"artist\":\"Artist\",\"trackinfo\":[]}'></div>"
                + "<div data-embed='{\"album_title\":\"Record\"}'></div>";
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override
            public Response execute(final Request request) {
                return new Response(200, "OK", Map.of(), album, request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final String url = "https://artist.bandcamp.com/album/record";
            final BandcampPlaylistExtractor extractor = new BandcampPlaylistExtractor(
                    new BandcampService(1), new ListLinkHandler(url, url, url, List.of(), ""));
            assertThrows(PaidContentException.class, () -> extractor.onFetchPage(offline));
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void usesAlbumArtForLargeTrackListsWithoutFetchingEachTrack() throws Exception {
        final StringBuilder tracks = new StringBuilder();
        for (int index = 1; index <= 10; index++) {
            if (index > 1) tracks.append(',');
            tracks.append("{\"title\":\"Song ").append(index)
                    .append("\",\"title_link\":\"/track/song").append(index)
                    .append("\",\"duration\":123}");
        }
        final String album = "<div data-tralbum='{" +
                "\"url\":\"https://artist.bandcamp.com/album/record\"," +
                "\"artist\":\"Artist\",\"art_id\":50,\"trackinfo\":[" + tracks + "]}'></div>" +
                "<div data-embed='{" + "\"album_title\":\"Record\"}'></div>";
        final List<String> requests = new ArrayList<>();
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override
            public Response execute(final Request request) {
                requests.add(request.url());
                return new Response(200, "OK", Map.of(), album, request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final String url = "https://artist.bandcamp.com/album/record";
            final BandcampPlaylistExtractor extractor = new BandcampPlaylistExtractor(
                    new BandcampService(1), new ListLinkHandler(url, url, url, List.of(), ""));
            extractor.onFetchPage(offline);

            final var items = extractor.getInitialPage().getItems();
            assertEquals(10, items.size());
            assertEquals("https://f4.bcbits.com/img/a50_10.jpg",
                    items.get(0).getThumbnails().get(0).getUrl());
            assertEquals(1, requests.size());
        } finally {
            VyonEx.init(previous);
        }
    }
}
