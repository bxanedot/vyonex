// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YoutubeClientVersionTest {
    @Test
    void readsVersionFromServiceWorker() throws Exception {
        final StubDownloader downloader = new StubDownloader();
        downloader.workerBody = "INNERTUBE_CONTEXT_CLIENT_VERSION\":\"2.20261022.01.00\"";

        assertEquals("2.20261022.01.00", withDownloader(downloader,
                YoutubeParsingHelper::getClientVersion));
        assertEquals(List.of("https://www.youtube.com/sw.js"), downloader.urls);
    }

    @Test
    void findsEcatcherVersionAfterCsiLookupMisses() throws Exception {
        final StubDownloader downloader = new StubDownloader();
        downloader.workerBody = "worker has no version";
        downloader.htmlBody = "window[\"ytInitialData\"] = {\"note\":\"brace { } and \\\"quote\\\"\","
                + "\"responseContext\":{"
                + "\"serviceTrackingParams\":[{\"service\":\"ECATCHER\",\"params\":["
                + "{\"key\":\"client.version\",\"value\":\"2.20261022.91\"}]}]}};";

        assertEquals("2.20261022.91", withDownloader(downloader,
                YoutubeParsingHelper::getClientVersion));
        assertEquals(List.of("https://www.youtube.com/sw.js",
                        "https://www.youtube.com/results?search_query=&ucbcb=1"),
                downloader.urls);
    }

    @Test
    void usesValidatedHardcodedVersionAfterDynamicSourcesFail() throws Exception {
        final StubDownloader downloader = new StubDownloader();
        downloader.workerBody = "worker has no version";
        downloader.htmlBody = "no initial data";
        downloader.guideBody = "x".repeat(6000);

        assertEquals(ClientsConstants.WEB_HARDCODED_CLIENT_VERSION,
                withDownloader(downloader, YoutubeParsingHelper::getClientVersion));
        assertEquals(List.of("https://www.youtube.com/sw.js",
                        "https://www.youtube.com/results?search_query=&ucbcb=1",
                        "https://www.youtube.com/youtubei/v1/guide?prettyPrint=false"),
                downloader.urls);
    }

    @Test
    void usesValidatedHardcodedMusicVersion() throws Exception {
        final StubDownloader downloader = new StubDownloader();
        downloader.guideBody = "x".repeat(501);

        assertEquals(ClientsConstants.WEB_REMIX_HARDCODED_CLIENT_VERSION,
                withDownloader(downloader, YoutubeParsingHelper::getYoutubeMusicClientVersion));
        final Request request = downloader.requests.get(0);
        final JsonObject payload = JsonParser.object().from(new String(
                request.dataToSend(), StandardCharsets.UTF_8));
        assertEquals(ClientsConstants.WEB_REMIX_HARDCODED_CLIENT_VERSION,
                payload.getObject("context").getObject("client").getString("clientVersion"));
        assertEquals(List.of(ClientsConstants.WEB_REMIX_HARDCODED_CLIENT_VERSION),
                request.headers().get("X-YouTube-Client-Version"));
        assertEquals(List.of("https://music.youtube.com/youtubei/v1/music/"
                        + "get_search_suggestions?prettyPrint=false"), downloader.urls);
    }

    @Test
    void triesMusicHomePageAfterWorkerScriptCannotSupplyAVersion() throws Exception {
        final StubDownloader downloader = new StubDownloader();
        downloader.guideBody = "{}";
        downloader.workerBody = "worker has no version";
        downloader.htmlBody = "INNERTUBE_CONTEXT_CLIENT_VERSION\":\"2.20261022.11.00\"";

        assertEquals("2.20261022.11.00",
                withDownloader(downloader, YoutubeParsingHelper::getYoutubeMusicClientVersion));
        assertEquals(List.of("https://music.youtube.com/youtubei/v1/music/"
                        + "get_search_suggestions?prettyPrint=false",
                        "https://music.youtube.com/sw.js", "https://music.youtube.com/?ucbcb=1"),
                downloader.urls);
    }

    private static <T> T withDownloader(final StubDownloader downloader,
            final Callable<T> task) throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        try {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(downloader);
            return task.call();
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static final class StubDownloader extends Downloader {
        private String workerBody = "";
        private String htmlBody = "";
        private String guideBody = "";
        private final List<String> urls = new ArrayList<>();
        private final List<Request> requests = new ArrayList<>();

        @Override
        public Response execute(final Request request) throws IOException, ReCaptchaException {
            requests.add(request);
            urls.add(request.url());
            if (request.url().endsWith("/sw.js")) {
                return response(request.url(), workerBody);
            }
            if (request.url().contains("ucbcb=1")) {
                return response(request.url(), htmlBody);
            }
            return response(request.url(), guideBody);
        }

        private static Response response(final String url, final String body) {
            return new Response(200, "OK", Map.of(), body, url);
        }
    }
}
