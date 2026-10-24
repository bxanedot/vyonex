// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.services.youtube.PoTokenProvider;
import com.vyonex.internal.fallback.services.youtube.PoTokenResult;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.services.youtube.YoutubeService;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeStreamFetchFlowTest {
    @Test
    void iosTokenProviderFailureDoesNotAbortOtherClientFallbacks() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        final StubDownloader downloader = new StubDownloader();
        try {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(downloader);
            YoutubeStreamExtractor.setFetchIosClient(true);
            YoutubeStreamExtractor.setPoTokenProvider(new PoTokenProvider() {
                @Override public PoTokenResult getWebClientPoToken(final String videoId) {
                    return null;
                }
                @Override public PoTokenResult getWebEmbedClientPoToken(final String videoId) {
                    return null;
                }
                @Override public PoTokenResult getAndroidClientPoToken(final String videoId) {
                    return new PoTokenResult("visitor-token", "player-token", null);
                }
                @Override public PoTokenResult getIosClientPoToken(final String videoId) {
                    throw new IllegalStateException("iOS token unavailable");
                }
            });
            final var link = YoutubeStreamLinkHandlerFactory.getInstance().fromUrl(
                    "https://www.youtube.com/watch?v=abcdefghijk");
            final var extractor = new YoutubeStreamExtractor(new YoutubeService(1), link);

            assertDoesNotThrow(() -> extractor.onFetchPage(downloader));
            assertTrue(downloader.requests.stream()
                    .filter(request -> request.dataToSend() != null)
                    .map(request -> new String(request.dataToSend(), StandardCharsets.UTF_8))
                    .anyMatch(body -> body.contains("\"clientName\":\"IOS\"")));
            assertTrue(downloader.requests.stream().anyMatch(
                    request -> request.url().contains("/next?")));
        } finally {
            YoutubeStreamExtractor.setPoTokenProvider(null);
            YoutubeStreamExtractor.setFetchIosClient(false);
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static final class StubDownloader extends Downloader {
        private final List<Request> requests = new ArrayList<>();

        @Override
        public Response execute(final Request request) throws IOException, ReCaptchaException {
            requests.add(request);
            final String body;
            if (request.url().endsWith("/sw.js")) {
                body = "INNERTUBE_CONTEXT_CLIENT_VERSION\":\"2.20261022.01.00\"";
            } else if (request.url().contains("/visitor_id?")) {
                body = "{\"responseContext\":{\"visitorData\":\"visitor-data\"},"
                        + "\"padding\":\"" + "x".repeat(60) + "\"}";
            } else if (request.url().contains("/next?")) {
                body = "{\"padding\":\"" + "x".repeat(60) + "\"}";
            } else {
                body = "{\"playabilityStatus\":{\"status\":\"OK\"},"
                        + "\"videoDetails\":{\"videoId\":\"abcdefghijk\","
                        + "\"lengthSeconds\":\"30\",\"thumbnail\":{\"thumbnails\":["
                        + "{\"url\":\"https://img.example/thumb.jpg\"}]}},"
                        + "\"microformat\":{\"playerMicroformatRenderer\":{}},"
                        + "\"padding\":\"" + "x".repeat(60) + "\"}";
            }
            return new Response(200, "OK", Map.of("Content-Type", List.of("application/json")),
                    body, request.url());
        }
    }
}
