// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.youtube.extractors.kiosk.YoutubeDesktopBaseKioskExtractor;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeDesktopBaseKioskExtractorContractTest {
    @Test
    void usesSelectedTabAndCarriesContinuationContextAcrossPages() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        YoutubeParsingHelper.resetClientVersion();
        final StubDownloader offline = new StubDownloader();
        try {
            VyonEx.init(offline);
            final YoutubeDesktopBaseKioskExtractor extractor = new YoutubeDesktopBaseKioskExtractor(
                    new YoutubeService(9), new ListLinkHandler(
                    "https://www.youtube.com/feed/live", "https://www.youtube.com/feed/live",
                    "", List.of(), ""), "Live", "UC123", "live-params");

            extractor.onFetchPage(offline);
            assertEquals("Live", extractor.getName());
            final var first = extractor.getInitialPage();
            assertEquals(List.of("Live one", "Live two"), names(first.getItems()));
            assertEquals("visitor-1", first.getNextPage().getId());
            assertTrue(body(first.getNextPage()).contains("\"continuation\":\"next-1\""));

            final var second = extractor.getPage(first.getNextPage());
            assertEquals(List.of("Live three"), names(second.getItems()));
            assertEquals("visitor-1", second.getNextPage().getId());
            assertTrue(body(second.getNextPage()).contains("\"continuation\":\"next-2\""));
            assertEquals(2, offline.browseRequests.size());
            assertTrue(body(offline.browseRequests.get(0)).contains("\"browseId\":\"UC123\""));
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    @Test
    void requiresAContinuationBodyForLaterPages() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        VyonEx.init(new Downloader() {
            @Override
            public Response execute(final Request request) throws IOException, ReCaptchaException {
                throw new IOException("Unexpected request: " + request.url());
            }
        });
        try {
            final YoutubeDesktopBaseKioskExtractor extractor = new YoutubeDesktopBaseKioskExtractor(
                    new YoutubeService(9), new ListLinkHandler("url", "url", "", List.of(), ""),
                    "Live", "UC123", "live-params");
            assertThrows(IllegalArgumentException.class, () -> extractor.getPage(new Page("url")));
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void readsItemsFromSectionShelves() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        YoutubeParsingHelper.resetClientVersion();
        final StubDownloader offline = new StubDownloader(StubDownloader.sectionResponse());
        try {
            VyonEx.init(offline);
            final YoutubeDesktopBaseKioskExtractor extractor = new YoutubeDesktopBaseKioskExtractor(
                    new YoutubeService(9), new ListLinkHandler("url", "url", "", List.of(), ""),
                    "Live", "UC123", "live-params");
            extractor.onFetchPage(offline);
            assertEquals(List.of("Shelf video"), names(extractor.getInitialPage().getItems()));
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static List<String> names(final List<StreamInfoItem> items) {
        return items.stream().map(StreamInfoItem::getName).toList();
    }

    private static String body(final Page page) {
        return new String(page.getBody(), StandardCharsets.UTF_8);
    }

    private static String body(final Request request) {
        return new String(request.dataToSend(), StandardCharsets.UTF_8);
    }

    private static JsonObject video(final String id, final String title) {
        return json("videoId", id, "title", json("simpleText", title),
                "lengthText", json("simpleText", "1:23"));
    }

    private static JsonObject item(final String id, final String title) {
        return json("gridVideoRenderer", video(id, title));
    }

    private static JsonObject continuation(final String token) {
        return json("continuationItemRenderer", json("continuationEndpoint",
                json("continuationCommand", json("token", token))));
    }

    private static JsonObject json(final Object... fields) {
        final JsonObject object = new JsonObject();
        for (int i = 0; i < fields.length; i += 2) {
            object.put((String) fields[i], fields[i + 1]);
        }
        return object;
    }

    private static final class StubDownloader extends Downloader {
        private final JsonObject initial;
        private final List<Request> browseRequests = new ArrayList<>();

        private StubDownloader() { this(initialResponse()); }

        private StubDownloader(final JsonObject initial) { this.initial = initial; }

        @Override
        public Response execute(final Request request) throws IOException, ReCaptchaException {
            if (request.url().endsWith("/sw.js")) {
                return new Response(200, "OK", Map.of(),
                        "INNERTUBE_CONTEXT_CLIENT_VERSION\":\"2.20261022.01.00\"", request.url());
            }
            browseRequests.add(request);
            final JsonObject response = browseRequests.size() == 1
                    ? initial : continuationResponse();
            return new Response(200, "OK", Map.of("Content-Type", List.of("application/json")),
                    JsonWriter.string(response), request.url());
        }

        private static JsonObject initialResponse() {
            final JsonObject wrongTab = json("tabRenderer", json("selected", false,
                    "content", json("richGridRenderer", json("contents", new JsonArray(
                            List.of(json("richItemRenderer", json("content", json("videoRenderer",
                                    video("wrong-video", "Wrong tab"))))))))));
            final JsonArray selectedItems = new JsonArray(List.of(
                    json("richItemRenderer", json("content", json("videoRenderer",
                            video("first-video", "Live one")))),
                    item("second-video", "Live two"), continuation("next-1")));
            final JsonObject selectedTab = json("tabRenderer", json("selected", true,
                    "content", json("richGridRenderer", json("contents", selectedItems))));
            return json("header", json("c4TabbedHeaderRenderer",
                            json("title", "Live")),
                    "responseContext", json("visitorData", "visitor-1"),
                    "contents", json("twoColumnBrowseResultsRenderer",
                            json("tabs", new JsonArray(List.of(wrongTab, selectedTab)))),
                    "padding", "x".repeat(100));
        }

        private static JsonObject continuationResponse() {
            return json("onResponseReceivedActions", new JsonArray(List.of(json(
                    "appendContinuationItemsAction", json("continuationItems", new JsonArray(List.of(
                            item("third-video", "Live three"), continuation("next-2"))))))),
                    "padding", "x".repeat(100));
        }

        private static JsonObject sectionResponse() {
            final JsonArray videos = new JsonArray(List.of(
                    json("videoRenderer", video("shelf-video", "Shelf video")),
                    continuation("shelf-next")));
            final JsonObject shelf = json("content", json("gridRenderer", json("items", videos)));
            final JsonObject section = json("itemSectionRenderer", json("contents",
                    new JsonArray(List.of(json("shelfRenderer", shelf)))));
            final JsonObject tab = json("tabRenderer", json("selected", true,
                    "endpoint", json("browseEndpoint", json("params", "live-params")),
                    "content", json("sectionListRenderer", json("contents",
                            new JsonArray(List.of(section))))));
            return json("header", json("c4TabbedHeaderRenderer", json("title", "Live")),
                    "responseContext", json("visitorData", "visitor-1"),
                    "contents", json("twoColumnBrowseResultsRenderer",
                            json("tabs", new JsonArray(List.of(tab)))),
                    "padding", "x".repeat(100));
        }
    }
}
