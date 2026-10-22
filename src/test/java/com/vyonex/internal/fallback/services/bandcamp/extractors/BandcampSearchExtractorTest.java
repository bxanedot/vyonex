// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Page;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
