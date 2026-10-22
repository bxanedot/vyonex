// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.services.bandcamp.BandcampService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BandcampSuggestionExtractorTest {
    @Test
    void postsTheQueryAndReturnsUniqueSuggestionNames() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        VyonEx.init(new Downloader() {
            @Override
            public Response execute(final Request request) {
                assertEquals("POST", request.httpMethod());
                assertEquals(BandcampExtractorHelper.BASE_API_URL
                        + "/bcsearch_public_api/1/autocomplete_elastic", request.url());
                final String body = new String(request.dataToSend(), StandardCharsets.UTF_8);
                assertTrue(body.contains("\"search_text\":\"ambient\""));
                return new Response(200, "OK", Map.of(),
                        "{\"auto\":{\"results\":[{\"name\":\"Ambient\"},"
                                + "{\"name\":\"Ambient\"},{\"name\":\"Ambient dub\"}]}}",
                        request.url());
            }
        });
        try {
            assertEquals(List.of("Ambient", "Ambient dub"), new BandcampSuggestionExtractor(
                    new BandcampService(1)).suggestionList("ambient"));
        } finally {
            VyonEx.init(previous);
        }
    }
}
