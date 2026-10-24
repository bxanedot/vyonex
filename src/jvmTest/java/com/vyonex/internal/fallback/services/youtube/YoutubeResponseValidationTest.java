// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.localization.Localization;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeResponseValidationTest {
    @Test
    void acceptsLongJsonResponsesAndRejectsShortOrHtmlBodies() throws Exception {
        final String body = "{\"data\":\"" + "x".repeat(60) + "\"}";
        final Response valid = new Response(200, "OK",
                Map.of("content-type", List.of("application/json")), body,
                "https://www.youtube.com/youtubei/v1/browse");
        assertEquals(body, YoutubeParsingHelper.getValidJsonResponseBody(valid));

        assertThrows(ParsingException.class, () -> YoutubeParsingHelper
                .getValidJsonResponseBody(new Response(200, "OK", Map.of(), "{}", valid.latestUrl())));
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper.getValidJsonResponseBody(
                new Response(200, "OK", Map.of("Content-Type", List.of("text/html")),
                        "x".repeat(80), valid.latestUrl())));
    }

    @Test
    void treatsNotFoundAndYoutubeErrorRedirectsAsUnavailable() {
        assertThrows(ContentNotAvailableException.class, () -> YoutubeParsingHelper
                .getValidJsonResponseBody(new Response(404, "Not Found", Map.of(),
                        "x".repeat(80), "https://www.youtube.com/watch?v=abc")));
        assertThrows(ContentNotAvailableException.class, () -> YoutubeParsingHelper
                .getValidJsonResponseBody(new Response(200, "OK", Map.of(),
                        "x".repeat(80), "https://www.youtube.com/oops")));
    }

    @Test
    void postsJsonRequestsWithTheExpectedQueryAndBodyOffline() throws Exception {
        final String responseBody = "{\"value\":\"" + "x".repeat(60) + "\"}";
        final StubDownloader downloader = new StubDownloader(responseBody);
        final Downloader previous = VyonEx.getDownloader();
        try {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(downloader);
            YoutubeParsingHelper.getJsonPostResponse("browse",
                    List.of("continuation=next", "foo=bar"),
                    "{\"request\":true}".getBytes(StandardCharsets.UTF_8), Localization.DEFAULT);

            assertEquals(List.of("https://www.youtube.com/sw.js",
                            "https://www.youtube.com/youtubei/v1/browse"
                            + "?continuation=next&foo=bar&prettyPrint=false"),
                    downloader.urls);
            assertEquals("POST", downloader.requests.get(1).httpMethod());
            assertEquals("{\"request\":true}", new String(
                    downloader.requests.get(1).dataToSend(), StandardCharsets.UTF_8));
            assertEquals("application/json", downloader.requests.get(1).headers()
                    .get("Content-Type").get(0));
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static final class StubDownloader extends Downloader {
        private final String body;
        private final List<String> urls = new ArrayList<>();
        private final List<Request> requests = new ArrayList<>();

        private StubDownloader(final String body) { this.body = body; }

        @Override
        public Response execute(final Request request) throws IOException, ReCaptchaException {
            urls.add(request.url());
            requests.add(request);
            if (request.url().endsWith("/sw.js")) {
                return new Response(200, "OK", Map.of(),
                        "INNERTUBE_CONTEXT_CLIENT_VERSION\":\"2.20261021.01.00\"",
                        request.url());
            }
            return new Response(200, "OK", Map.of("Content-Type", List.of("application/json")),
                    body, request.url());
        }
    }
}
