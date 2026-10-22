// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.services.bandcamp.BandcampService;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BandcampStreamExtractorTest {
    private static final String PAGE_DATA = "{\"url\":\"http://artist.bandcamp.com/track/song\","
            + "\"artist\":\"Artist\",\"art_id\":77,\"trackinfo\":[{"
            + "\"file\":{\"mp3-128\":\"https://audio.example/song.mp3\"},"
            + "\"duration\":123.5}],\"current\":{\"title\":\"Song\","
            + "\"publish_date\":\"15 Jun 2024 12:30:00 UTC\",\"about\":\"Story\","
            + "\"lyrics\":\"Words\",\"credits\":\"Player\"}}";

    @Test
    void extractsPublicTrackFieldsFromAnOfflinePage() throws Exception {
        final String html = "<html><body><div data-tralbum='" + PAGE_DATA + "'></div>"
                + "<img class='band-photo' src='https://img.example/artist.jpg'>"
                + "<div class='tralbum-tags'><a class='tag'>Ambient</a></div>"
                + "<div id='license'>CC BY</div><span itemprop='keywords'>ambient</span></body></html>";
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override
            public Response execute(final Request request) {
                return new Response(200, "OK", Map.of(), html, request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final StreamingService service = new BandcampService(1);
            final LinkHandler link = new LinkHandler("https://artist.bandcamp.com/track/song",
                    "https://artist.bandcamp.com/track/song", "song");
            final BandcampStreamExtractor extractor = new BandcampStreamExtractor(service, link);
            extractor.onFetchPage(offline);

            assertEquals("Song", extractor.getName());
            assertEquals("https://artist.bandcamp.com/track/song", extractor.getUrl());
            assertEquals("https://artist.bandcamp.com/", extractor.getUploaderUrl());
            assertEquals(123L, extractor.getLength());
            assertEquals("https://audio.example/song.mp3",
                    extractor.getAudioStreams().get(0).getContent());
            assertEquals("Story\n\nWords\n\nPlayer", extractor.getDescription().getContent());
            assertEquals("CC BY", extractor.getLicence());
            assertEquals(List.of("ambient"), extractor.getTags());
            assertEquals("2024-06-15T12:30:00Z", extractor.getUploadDate().getInstant().toString());
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void reportsMissingAndMalformedEmbeddedData() {
        assertThrows(ParsingException.class, () -> BandcampStreamExtractor.getAlbumInfoJson(""));
        assertThrows(ParsingException.class, () -> BandcampStreamExtractor.getAlbumInfoJson(
                "<div data-tralbum='not json'></div>"));
    }

    @Test
    void mapsRelatedAlbumMarkup() {
        final var album = new BandcampRelatedPlaylistInfoItemExtractor(Jsoup.parse(
                "<div class='recommended-album'><a class='album-link' "
                        + "href='http://artist.bandcamp.com/album/record'></a>"
                        + "<span class='release-title'>Record</span>"
                        + "<span class='by-artist'>by Artist</span>"
                        + "<img class='album-art' src='https://img.example/cover.jpg'></div>")
                .selectFirst(".recommended-album"));

        assertEquals("Record", album.getName());
        assertEquals("https://artist.bandcamp.com/album/record", album.getUrl());
        assertEquals("Artist", album.getUploaderName());
        assertEquals("https://img.example/cover.jpg", album.getThumbnails().get(0).getUrl());
    }
}
