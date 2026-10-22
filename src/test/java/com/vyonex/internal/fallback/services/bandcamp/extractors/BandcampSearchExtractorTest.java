// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem.BandcampSearchStreamInfoItemExtractor;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
