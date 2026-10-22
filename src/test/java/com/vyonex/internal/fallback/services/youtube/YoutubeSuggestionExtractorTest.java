// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeSuggestionExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeSuggestionExtractorTest {
    @Test
    void requestsLocalizedSuggestionsAndRejectsBadResponsesOffline() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        try {
            final CapturingDownloader valid = new CapturingDownloader(new Response(200, "OK",
                    Map.of("Content-Type", List.of("application/json; charset=UTF-8")),
                    "[\"ambient & jazz\",[[\"Ambient\",1],[\"   \",2],"
                            + "[\"Ambient dub\",3],{},[\"Ambient\",4]],[]]",
                    "https://suggestqueries-clients6.youtube.com/complete/search"));
            VyonEx.init(valid);
            final YoutubeSuggestionExtractor extractor = new YoutubeSuggestionExtractor(
                    new YoutubeService(1));
            extractor.forceLocalization(new Localization("fr", "CH"));
            extractor.forceContentCountry(new ContentCountry("CH"));
            final List<String> suggestions = extractor.suggestionList("ambient & jazz");
            assertEquals(List.of("Ambient", "Ambient dub", "Ambient"), suggestions);
            assertThrows(UnsupportedOperationException.class,
                    () -> suggestions.add("mutable"));
            assertEquals("https://suggestqueries-clients6.youtube.com/complete/search"
                            + "?client=youtube&ds=yt&gl=CH&q=ambient+%26+jazz&xhr=t",
                    valid.request.url());
            assertTrue(valid.request.headers().containsKey("Origin"));

            VyonEx.init(new CapturingDownloader(new Response(200, "OK",
                    Map.of("Content-Type", List.of("text/html")), "<html>",
                    "https://suggestqueries-clients6.youtube.com/complete/search")));
            assertThrows(ExtractionException.class, () -> extractor.suggestionList("x"));

            VyonEx.init(new CapturingDownloader(new Response(200, "OK",
                    Map.of("Content-Type", List.of("application/json")), "", "https://example.org")));
            assertThrows(ExtractionException.class, () -> extractor.suggestionList("x"));

            VyonEx.init(new CapturingDownloader(new Response(200, "OK",
                    Map.of("Content-Type", List.of("application/json")), "not json",
                    "https://example.org")));
            assertThrows(ParsingException.class, () -> extractor.suggestionList("x"));
        } finally {
            VyonEx.init(previous);
        }
    }

    private static final class CapturingDownloader extends Downloader {
        private final Response response;
        private Request request;

        private CapturingDownloader(final Response response) { this.response = response; }

        @Override
        public Response execute(final Request value) throws IOException {
            request = value;
            return response;
        }
    }
}
