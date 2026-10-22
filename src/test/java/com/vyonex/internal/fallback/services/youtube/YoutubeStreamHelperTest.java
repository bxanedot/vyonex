// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeStreamHelperTest {
    @Test
    void requestsPlayerResponsesForSupportedClientsOffline() throws Exception {
        final StubDownloader downloader = new StubDownloader();
        final Downloader previous = VyonEx.getDownloader();
        try {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(downloader);
            final PoTokenResult token = new PoTokenResult("visitor-token", "player-token", null);
            final String videoId = "video1234567";

            YoutubeStreamHelper.getWebMetadataPlayerResponse(
                    Localization.DEFAULT, ContentCountry.DEFAULT, videoId);
            YoutubeStreamHelper.getWebEmbeddedPlayerResponse(Localization.DEFAULT,
                    ContentCountry.DEFAULT, videoId, "cpn-web", token, 12345);
            YoutubeStreamHelper.getAndroidPlayerResponse(ContentCountry.DEFAULT,
                    Localization.DEFAULT, videoId, "cpn-android", token);
            assertNotNull(YoutubeStreamHelper.getAndroidReelPlayerResponse(
                    ContentCountry.DEFAULT, Localization.DEFAULT, videoId, "cpn-reel"));
            YoutubeStreamHelper.getIosPlayerResponse(ContentCountry.DEFAULT,
                    Localization.DEFAULT, videoId, "cpn-ios", null);
            YoutubeStreamHelper.getVisionOsPlayerResponse(ContentCountry.DEFAULT,
                    Localization.DEFAULT, videoId, "cpn-vision");

            assertEquals(11, downloader.requests.size());
            assertEquals("https://www.youtube.com/sw.js", downloader.requests.get(0).url());
            assertTrue(downloader.requests.get(1).url().endsWith("/visitor_id?prettyPrint=false"));
            assertTrue(downloader.requests.get(2).url().contains("$fields=microformat,"));
            assertTrue(downloader.requests.get(3).url().endsWith("/player?prettyPrint=false"));
            assertTrue(downloader.requests.get(4).url().contains("/player?prettyPrint=false&t="));
            assertTrue(downloader.requests.get(5).url().endsWith("/visitor_id?prettyPrint=false"));
            assertTrue(downloader.requests.get(6).url().contains("/reel/reel_item_watch?"));
            assertTrue(downloader.requests.get(7).url().endsWith("/visitor_id?prettyPrint=false"));
            assertTrue(downloader.requests.get(8).url().contains("/player?prettyPrint=false&t="));
            assertTrue(downloader.requests.get(9).url().endsWith("/visitor_id?prettyPrint=false"));
            assertTrue(downloader.requests.get(10).url().contains("/player?prettyPrint=false&t="));

            final JsonObject webBody = requestBody(downloader.requests.get(2));
            assertEquals(videoId, webBody.getString("videoId"));
            assertTrue(webBody.getBoolean("contentCheckOk"));
            assertTrue(webBody.getBoolean("racyCheckOk"));
            assertEquals("visitor-data", webBody.getObject("context").getObject("client")
                    .getString("visitorData"));

            final JsonObject embeddedBody = requestBody(downloader.requests.get(3));
            assertEquals("https://www.youtube.com/watch?v=" + videoId,
                    embeddedBody.getObject("context").getObject("thirdParty")
                            .getString("embedUrl"));
            assertEquals(12345, embeddedBody.getObject("playbackContext")
                    .getObject("contentPlaybackContext").getInt("signatureTimestamp"));
            assertEquals("player-token", embeddedBody.getObject("serviceIntegrityDimensions")
                    .getString("poToken"));

            final JsonObject androidBody = requestBody(downloader.requests.get(4));
            assertEquals("ANDROID", androidBody.getObject("context").getObject("client")
                    .getString("clientName"));
            assertEquals("player-token", androidBody.getObject("serviceIntegrityDimensions")
                    .getString("poToken"));
            final JsonObject reelBody = requestBody(downloader.requests.get(6));
            assertEquals(videoId, reelBody.getObject("playerRequest").getString("videoId"));
            assertFalse(reelBody.getBoolean("disablePlayerResponse"));

            assertTrue(downloader.requests.get(2).headers().containsKey("Cookie"));
            assertEquals("2", downloader.requests.get(4).headers()
                    .get("X-Goog-Api-Format-Version").get(0));
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static JsonObject requestBody(final Request request) throws Exception {
        return JsonParser.object().from(new String(request.dataToSend(), StandardCharsets.UTF_8));
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
            } else if (request.url().contains("/reel/reel_item_watch?")) {
                body = "{\"playerResponse\":{\"playabilityStatus\":{\"status\":\"OK\"},"
                        + "\"padding\":\"" + "x".repeat(60) + "\"},"
                        + "\"padding\":\"" + "x".repeat(60) + "\"}";
            } else {
                body = "{\"playabilityStatus\":{\"status\":\"OK\"},"
                        + "\"padding\":\"" + "x".repeat(60) + "\"}";
            }
            return new Response(200, "OK", Map.of("Content-Type", List.of("application/json")),
                    body, request.url());
        }
    }
}
