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
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeRequestConstructionTest {
    @Test
    void buildsClientAndDeviceContextWithOptionalEmbedUrl() {
        final JsonObject body = YoutubeParsingHelper.prepareJsonBuilder(
                new Localization("de", "CH"), new ContentCountry("AT"),
                InnertubeClientRequestInfo.ofAndroidClient(), "https://embed.example/video")
                .done();
        final JsonObject context = body.getObject("context");
        final JsonObject client = context.getObject("client");
        assertEquals("ANDROID", client.getString("clientName"));
        assertEquals("de-CH", client.getString("hl"));
        assertEquals("AT", client.getString("gl"));
        assertEquals("MOBILE", client.getString("platform"));
        assertEquals("Android", client.getString("osName"));
        assertEquals(36, client.getInt("androidSdkVersion"));
        assertEquals("https://embed.example/video",
                context.getObject("thirdParty").getString("embedUrl"));
        assertTrue(context.getObject("request").getBoolean("useSsl"));
        assertFalse(context.getObject("user").getBoolean("lockedSafetyMode"));
        assertTrue(context.getObject("request").getArray("internalExperimentFlags").isEmpty());
        assertFalse(client.containsKey("deviceMake"));
    }

    @Test
    void fetchesVisitorDataFromRequestedEndpointOffline() throws Exception {
        final String responseBody = "{\"responseContext\":{\"visitorData\":\"visitor-42\"},"
                + "\"padding\":\"" + "x".repeat(60) + "\"}";
        final StubDownloader downloader = new StubDownloader(responseBody);
        final Downloader previous = VyonEx.getDownloader();
        try {
            VyonEx.init(downloader);
            final Map<String, List<String>> headers = Map.of("Authorization",
                    List.of("Bearer test"));
            final String endpoint = "https://youtubei.googleapis.com/youtubei/v1/";
            assertEquals("visitor-42", YoutubeParsingHelper.getVisitorDataFromInnertube(
                    InnertubeClientRequestInfo.ofIosClient(), Localization.DEFAULT,
                    ContentCountry.DEFAULT, headers, endpoint, null, false));
            assertEquals("visitor-42", YoutubeParsingHelper.getVisitorDataFromInnertube(
                    InnertubeClientRequestInfo.ofIosClient(), Localization.DEFAULT,
                    ContentCountry.DEFAULT, headers, endpoint, null, true));

            assertEquals(List.of(endpoint + "visitor_id?prettyPrint=false",
                    endpoint + "guide?prettyPrint=false"), downloader.requests.stream()
                    .map(Request::url).toList());
            final JsonObject sentBody = JsonParser.object().from(new String(
                    downloader.requests.get(0).dataToSend(), StandardCharsets.UTF_8));
            assertEquals("IOS", sentBody.getObject("context").getObject("client")
                    .getString("clientName"));
            assertEquals(List.of("Bearer test"), downloader.requests.get(0).headers()
                    .get("Authorization"));
        } finally {
            VyonEx.init(previous);
        }
    }

    private static final class StubDownloader extends Downloader {
        private final String body;
        private final List<Request> requests = new ArrayList<>();

        private StubDownloader(final String body) { this.body = body; }

        @Override
        public Response execute(final Request request) throws IOException, ReCaptchaException {
            requests.add(request);
            return new Response(200, "OK", Map.of("Content-Type", List.of("application/json")),
                    body, request.url());
        }
    }
}
