// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.bandcamp.BandcampService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BandcampCommentsExtractorTest {
    @Test
    void loadsReviewPagesWithTheReturnedCursor() throws Exception {
        final String html = "<meta name='bc-page-properties' content='{"
                + "\"item_id\":123}'>"
                + "<div id='collectors-data' data-blob='{" +
                "\"reviews\":[{\"why\":\"Nice release\",\"name\":\"Fan\","
                + "\"image_id\":45,\"token\":\"cursor-one\"},{\"why\":\"Second initial\"}],"
                + "\"more_reviews_available\":true}'></div>";
        final String next = "{\"results\":[{\"why\":\"Second review\","
                + "\"name\":\"Another fan\",\"token\":\"cursor-two\"}],"
                + "\"more_available\":false}";
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override
            public Response execute(final Request request) {
                if ("POST".equals(request.httpMethod())) {
                    final String body = new String(request.dataToSend(), StandardCharsets.UTF_8);
                    assertTrue(body.contains("\"token\":\"cursor-one\""));
                    return new Response(200, "OK", Map.of(), next, request.url());
                }
                return new Response(200, "OK", Map.of(), html, request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final String url = "https://artist.bandcamp.com/track/song";
            final BandcampCommentsExtractor extractor = new BandcampCommentsExtractor(
                    new BandcampService(1), new ListLinkHandler(url, url, url, List.of(), ""));
            extractor.onFetchPage(offline);
            final var first = extractor.getInitialPage();
            assertEquals(2, first.getItems().size());
            assertEquals("Nice release", first.getItems().get(0).getName());
            assertEquals("Fan", first.getItems().get(0).getUploaderName());
            assertEquals(List.of("123", "cursor-one"), first.getNextPage().getIds());
            assertFalse(extractor.isCommentsDisabled());

            final var second = extractor.getPage(new Page(first.getNextPage().getIds()));
            assertEquals("Second review", second.getItems().get(0).getName());
            assertNull(second.getNextPage());
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void wrapsMalformedReviewApiDataAsParsingFailure() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override
            public Response execute(final Request request) {
                return new Response(200, "OK", Map.of(), "not-json", request.url());
            }
        };
        try {
            VyonEx.init(offline);
            final String url = "https://artist.bandcamp.com/track/song";
            final BandcampCommentsExtractor extractor = new BandcampCommentsExtractor(
                    new BandcampService(1), new ListLinkHandler(url, url, url, List.of(), ""));

            assertThrows(ParsingException.class,
                    () -> extractor.getPage(new Page(List.of("123", "cursor-one"))));
        } finally {
            VyonEx.init(previous);
        }
    }
}
