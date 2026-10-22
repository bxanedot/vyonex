// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.services.youtube.extractors.kiosk.YoutubeTrendingExtractor;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeTrendingLinkHandlerFactory;
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

class YoutubeTrendingExtractorTest {
    private static final String VIDEO_TAB_PARAMS = "4gIOGgxtb3N0X3BvcHVsYXI%3D";

    @Test
    void mapsSelectedGridAndShelfVideosFromOfflineResponses() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        try {
            final StubDownloader gridDownloader = new StubDownloader(response(
                    json("richGridRenderer", json("contents", new JsonArray(List.of(
                            json("richItemRenderer", json("content", json("videoRenderer",
                                    video("abcdefghijk", "Grid video")))),
                            json("continuationItemRenderer", json()))))), VIDEO_TAB_PARAMS));
            VyonEx.init(gridDownloader);
            final YoutubeTrendingExtractor grid = extractor();
            grid.onFetchPage(gridDownloader);
            assertEquals("Trending", grid.getName());
            assertEquals(List.of("Grid video"), names(grid.getInitialPage().getItems()));
            assertEquals("FEtrending", body(gridDownloader.browseRequest).getString("browseId"));
            assertEquals(VIDEO_TAB_PARAMS, body(gridDownloader.browseRequest).getString("params"));

            final StubDownloader shelvesDownloader = new StubDownloader(response(
                    sections(shelf("Shorts", video("abcdefghijk", "Short")),
                            shelf(null, video("lmnopqrstuv", "Trending video"))),
                    "trending-tab"));
            VyonEx.init(shelvesDownloader);
            final YoutubeTrendingExtractor shelves = extractor();
            shelves.onFetchPage(shelvesDownloader);
            assertEquals(List.of("Trending video"), names(shelves.getInitialPage().getItems()));

            final StubDownloader videoTabDownloader = new StubDownloader(response(
                    sections(shelf("Shorts", video("abcdefghijk", "First shelf")),
                            shelf(null, video("lmnopqrstuv", "Second shelf"))), VIDEO_TAB_PARAMS));
            VyonEx.init(videoTabDownloader);
            final YoutubeTrendingExtractor videoTab = extractor();
            videoTab.onFetchPage(videoTabDownloader);
            assertEquals(List.of("First shelf"), names(videoTab.getInitialPage().getItems()));
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static YoutubeTrendingExtractor extractor() throws Exception {
        return new YoutubeTrendingExtractor(new YoutubeService(1),
                YoutubeTrendingLinkHandlerFactory.INSTANCE.fromUrl(
                        "https://www.youtube.com/feed/trending"), YoutubeTrendingExtractor.KIOSK_ID);
    }

    private static JsonObject response(final JsonObject content, final String params) {
        final JsonObject tabRenderer = json("selected", true,
                "endpoint", json("browseEndpoint", json("params", params)),
                "content", content);
        final JsonArray tabs = new JsonArray(List.of(json("tabRenderer", tabRenderer)));
        return json("header", json("feedTabbedHeaderRenderer",
                        json("title", json("simpleText", "Trending"))),
                "contents", json("twoColumnBrowseResultsRenderer", json("tabs", tabs)),
                "padding", "x".repeat(60));
    }

    private static JsonObject sections(final JsonObject... shelves) {
        final JsonArray shelfEntries = new JsonArray();
        for (final JsonObject shelf : shelves) {
            shelfEntries.add(json("shelfRenderer", shelf));
        }
        return json("sectionListRenderer", json("contents", new JsonArray(List.of(
                json("itemSectionRenderer", json("contents", shelfEntries))))));
    }

    private static JsonObject shelf(final String title, final JsonObject video) {
        final JsonObject shelf = new JsonObject();
        if (title != null) shelf.put("title", json("simpleText", title));
        final JsonObject videoItem = json("videoRenderer", video);
        final JsonArray videoItems = new JsonArray(List.of(videoItem));
        shelf.put("content", json("expandedShelfContentsRenderer", json("items", videoItems)));
        return shelf;
    }

    private static JsonObject video(final String id, final String title) {
        return json("videoId", id, "title", json("simpleText", title),
                "lengthText", json("simpleText", "1:23"));
    }

    private static JsonObject json(final Object... fields) {
        final JsonObject object = new JsonObject();
        for (int index = 0; index < fields.length; index += 2) {
            object.put((String) fields[index], fields[index + 1]);
        }
        return object;
    }

    private static List<String> names(final List<StreamInfoItem> items) {
        return items.stream().map(StreamInfoItem::getName).toList();
    }

    private static JsonObject body(final Request request) throws Exception {
        return com.vyonex.internal.json.JsonParser.object().from(new String(
                request.dataToSend(), StandardCharsets.UTF_8));
    }

    private static final class StubDownloader extends Downloader {
        private final String body;
        private final List<Request> requests = new ArrayList<>();
        private Request browseRequest;

        private StubDownloader(final JsonObject response) {
            body = JsonWriter.string(response);
        }

        @Override
        public Response execute(final Request request) throws IOException, ReCaptchaException {
            requests.add(request);
            if (request.url().endsWith("/sw.js")) {
                return new Response(200, "OK", Map.of(),
                        "INNERTUBE_CONTEXT_CLIENT_VERSION\":\"2.20261022.01.00\"",
                        request.url());
            }
            browseRequest = request;
            return new Response(200, "OK", Map.of("Content-Type", List.of("application/json")),
                    body, request.url());
        }
    }
}
