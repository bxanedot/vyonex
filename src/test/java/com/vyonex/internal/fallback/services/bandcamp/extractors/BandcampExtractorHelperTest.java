// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BandcampExtractorHelperTest {
    @Test
    void identifiesArtistAndRadioUrls() throws Exception {
        assertTrue(BandcampExtractorHelper.isArtistDomain("https://artist.bandcamp.com/"));
        assertFalse(BandcampExtractorHelper.isArtistDomain("https://bandcamp.com/"));
        assertTrue(BandcampExtractorHelper.isRadioUrl("https://bandcamp.com/?show=123"));
        assertFalse(BandcampExtractorHelper.isRadioUrl("https://example.test/?show=123"));
        assertFalse(BandcampExtractorHelper.isRadioUrl("https://bandcamp.com/?show=abc"));
    }

    @Test
    void identifiesCustomArtistDomainsByTheirCartLink() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override public Response execute(final Request request) {
                final String html = request.url().contains("shop.example")
                        ? "<div class='cart-wrapper'><a href='https://bandcamp.com/cart'>Cart</a></div>"
                        : "<main>Not an artist page</main>";
                return new Response(200, "OK", Map.of(), html, request.url());
            }
        };
        try {
            VyonEx.init(offline);
            assertTrue(BandcampExtractorHelper.isArtistDomain("https://shop.example/"));
            assertFalse(BandcampExtractorHelper.isArtistDomain("https://plain.example/"));
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void returnsOnlyImageUrlsProvidedByBandcamp() {
        assertEquals("https://f4.bcbits.com/img/a42_10.jpg",
                BandcampExtractorHelper.getImageUrl(42, true));
        assertTrue(BandcampExtractorHelper.getImagesFromImageId(0, true).isEmpty());
        final Image image = BandcampExtractorHelper.getImagesFromSearchResult(
                Jsoup.parse("<div class='art'><img src='//f4.bcbits.com/img/42_10.jpg'></div>"))
                .get(0);
        assertEquals("https://f4.bcbits.com/img/42_10.jpg", image.getUrl());
        assertEquals(Image.ResolutionLevel.UNKNOWN, image.getEstimatedResolutionLevel());
    }

    @Test
    void parsesBandcampPublishDates() throws Exception {
        assertEquals(Instant.parse("2024-06-15T12:30:00Z"),
                BandcampExtractorHelper.parseDate("15 Jun 2024 12:30:00 UTC").getInstant());
        assertThrows(ParsingException.class,
                () -> BandcampExtractorHelper.parseDate("not a date"));
    }
}
