// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.services.bandcamp.BandcampService;
import com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem.BandcampSearchStreamInfoItemExtractor;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BandcampSearchExtractorTest {

    @Test
    void followsTheNextPageLink() {
        final Page next = BandcampSearchExtractor.findNextPage(
                Jsoup.parse("<div class='pagelist'><ul>"
                        + "<li class='selected'><span>1</span></li>"
                        + "<li><a href='?q=ambient&amp;page=2'>2</a></li>"
                        + "</ul></div>"),
                "https://bandcamp.com/search?q=ambient&page=1");

        assertEquals("https://bandcamp.com/search?q=ambient&page=2", next.getUrl());
    }

    @Test
    void hasNoNextPageWithoutPagination() {
        assertNull(BandcampSearchExtractor.findNextPage(
                Jsoup.parse("<div class='searchresult'>result</div>"),
                "https://bandcamp.com/search?q=ambient&page=1"));
    }

    @Test
    void rejectsPaginationLinksToAnotherHost() {
        assertNull(BandcampSearchExtractor.findNextPage(
                Jsoup.parse("<div class='pagelist'><a class='next' "
                        + "href='https://elsewhere.example/next'>Next</a></div>"),
                "https://bandcamp.com/search?q=ambient&page=1"));
    }

    @Test
    void rejectsPaginationLinksThatOnlyChangeTheFragment() {
        assertNull(BandcampSearchExtractor.findNextPage(
                Jsoup.parse("<div class='pagelist'><a rel='next' href='#top'>Next</a></div>"),
                "https://bandcamp.com/search?q=ambient&page=1"));
    }

    @Test
    void rejectsPaginationLinksToAnotherPort() {
        assertNull(BandcampSearchExtractor.findNextPage(
                Jsoup.parse("<div class='pagelist'><a rel='next' "
                        + "href='https://bandcamp.com:8443/search?page=2'>Next</a></div>"),
                "https://bandcamp.com/search?q=ambient&page=1"));
    }

    @Test
    void mapsSearchTrackAndAlbumResults() throws Exception {
        final var result = Jsoup.parse("<div class='searchresult'><div class='art'>"
                + "<img src='https://img.example/cover.jpg'></div><div class='result-info'>"
                + "<div class='heading'>Song</div><div class='itemurl'>"
                + "https://artist.bandcamp.com/track/song</div><div class='subhead'>Track by Artist</div>"
                + "</div></div>").selectFirst(".searchresult");
        final var track = new BandcampSearchStreamInfoItemExtractor(result, null);

        assertEquals("Song", track.getName());
        assertEquals("https://artist.bandcamp.com/track/song", track.getUrl());
        assertEquals("Artist", track.getUploaderName());
        assertEquals("https://img.example/cover.jpg", track.getThumbnails().get(0).getUrl());
        assertEquals(StreamType.AUDIO_STREAM, track.getStreamType());
        assertEquals(-1, track.getViewCount());
        assertFalse(track.isUploaderVerified());

        final var album = new BandcampPlaylistInfoItemExtractor(Jsoup.parse(
                "<div class='searchresult'><div class='art'><img src='https://img.example/a.jpg'></div>"
                        + "<div class='result-info'><div class='heading'>Record</div>"
                        + "<div class='itemurl'>https://artist.bandcamp.com/album/record</div>"
                        + "<div class='subhead'>Album by Artist</div>"
                        + "<div class='length'>12 tracks</div></div></div>")
                .selectFirst(".searchresult"));
        assertEquals("Record", album.getName());
        assertEquals("https://artist.bandcamp.com/album/record", album.getUrl());
        assertEquals("Artist", album.getUploaderName());
        assertEquals(12, album.getStreamCount());
    }

    @Test
    void mapsSearchArtistResults() throws Exception {
        final var artist = new BandcampChannelInfoItemExtractor(Jsoup.parse(
                "<div class='searchresult'><div class='result-info'>"
                        + "<div class='heading'>Artist</div>"
                        + "<div class='itemurl'>https://artist.bandcamp.com/</div>"
                        + "<div class='subhead'>electronic</div></div></div>")
                .selectFirst(".searchresult"));

        assertEquals("Artist", artist.getName());
        assertEquals("https://artist.bandcamp.com/", artist.getUrl());
        assertEquals("electronic", artist.getDescription());
        assertEquals(-1, artist.getSubscriberCount());
    }

    @Test
    void fetchesAndMapsOnlySupportedSearchResultKinds() throws Exception {
        final String html = "<div class='searchresult'><div class='result-info'>"
                + "<div class='itemtype'>Artist</div><div class='heading'>Artist</div>"
                + "<div class='itemurl'>https://artist.bandcamp.com/</div></div></div>"
                + "<div class='searchresult'><div class='result-info'>"
                + "<div class='itemtype'>Album</div><div class='heading'>Album</div>"
                + "<div class='itemurl'>https://artist.bandcamp.com/album/album</div>"
                + "<div class='length'>8 tracks</div></div></div>"
                + "<div class='searchresult'><div class='result-info'>"
                + "<div class='itemtype'>Track</div><div class='heading'>Track</div>"
                + "<div class='itemurl'>https://artist.bandcamp.com/track/track</div></div></div>"
                + "<div class='searchresult'><div class='result-info'>"
                + "<div class='itemtype'>Fan</div><div class='heading'>Ignored</div></div></div>"
                + "<div class='pagelist'><a rel='next' href='?q=ambient&amp;page=2'>Next</a></div>";
        final Downloader previous = VyonEx.getDownloader();
        final String expectedUrl = "https://bandcamp.com/search?q=ambient&page=1";
        VyonEx.init(new Downloader() {
            @Override
            public Response execute(final Request request) {
                assertEquals(expectedUrl, request.url());
                assertEquals(List.of("identity"), request.headers().get("Cookie"));
                return new Response(200, "OK", Map.of(), html, request.url());
            }
        });
        try {
            final var page = new BandcampService(1).getSearchExtractor("ambient", List.of(), "")
                    .getInitialPage();
            assertEquals(List.of("Artist", "Album", "Track"),
                    page.getItems().stream().map(item -> item.getName()).toList());
            assertEquals(List.of("CHANNEL", "PLAYLIST", "STREAM"),
                    page.getItems().stream().map(item -> item.getInfoType().name()).toList());
            assertTrue(page.hasNextPage());
            assertEquals("https://bandcamp.com/search?q=ambient&page=2",
                    page.getNextPage().getUrl());
        } finally {
            VyonEx.init(previous);
        }
    }
}
