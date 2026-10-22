// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeSearchExtractor;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeSearchExtractorTest {
    @Test
    void parsesSearchResultsAndContinuationOffline() throws Exception {
        final JsonObject initial = searchResponse(new JsonArray(List.of(
                json("didYouMeanRenderer", json("correctedQueryEndpoint", json("searchEndpoint",
                        json("query", "ambient band")))),
                json("videoRenderer", video("abcdefghijk", "Video result")),
                json("channelRenderer", channel("UC123", "Channel result")))), "next-token");
        final JsonObject continuation = continuationResponse(
                json("videoRenderer", video("lmnopqrstuv", "Next result")), "last-token");
        final StubDownloader downloader = new StubDownloader(List.of(
                JsonWriter.string(initial), JsonWriter.string(continuation)));
        final Downloader previous = VyonEx.getDownloader();
        try {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(downloader);
            final YoutubeSearchExtractor extractor = extractor(YoutubeSearchQueryHandlerFactory.ALL);
            extractor.onFetchPage(downloader);

            assertEquals("ambient band", extractor.getSearchSuggestion());
            assertFalse(extractor.isCorrectedSearch());
            assertTrue(extractor.getMetaInfo().isEmpty());
            assertTrue(extractor.getUrl().contains("&gl=GB"));
            final var firstPage = extractor.getInitialPage();
            assertEquals(List.of("Video result", "Channel result"), firstPage.getItems().stream()
                    .map(InfoItem::getName).toList());
            assertTrue(firstPage.hasNextPage());
            assertEquals("next-token", firstPage.getNextPage().getId());

            final var nextPage = extractor.getPage(firstPage.getNextPage());
            assertEquals(List.of("Next result"), nextPage.getItems().stream()
                    .map(InfoItem::getName).toList());
            assertEquals("last-token", nextPage.getNextPage().getId());
            assertEquals("ambient", requestBody(downloader.requests.get(1)).getString("query"));
            assertEquals("next-token", requestBody(downloader.requests.get(2))
                    .getString("continuation"));
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    @Test
    void appliesVideoOnlySearchFilter() throws Exception {
        final JsonObject initial = searchResponse(new JsonArray(List.of(
                json("videoRenderer", video("abcdefghijk", "Video result")),
                json("channelRenderer", channel("UC123", "Channel result")))), null);
        final StubDownloader downloader = new StubDownloader(List.of(JsonWriter.string(initial)));
        final Downloader previous = VyonEx.getDownloader();
        try {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(downloader);
            final YoutubeSearchExtractor extractor = extractor(
                    YoutubeSearchQueryHandlerFactory.VIDEOS);
            extractor.onFetchPage(downloader);
            assertEquals(List.of("Video result"), extractor.getInitialPage().getItems().stream()
                    .map(InfoItem::getName).toList());
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    @Test
    void reportsCorrectedSearchText() throws Exception {
        final JsonObject initial = searchResponse(new JsonArray(List.of(
                json("showingResultsForRenderer", json("correctedQuery",
                        json("simpleText", "ambient band"))))), null);
        final StubDownloader downloader = new StubDownloader(List.of(JsonWriter.string(initial)));
        final Downloader previous = VyonEx.getDownloader();
        try {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(downloader);
            final YoutubeSearchExtractor extractor = extractor(YoutubeSearchQueryHandlerFactory.ALL);
            extractor.onFetchPage(downloader);
            assertTrue(extractor.isCorrectedSearch());
            assertEquals("ambient band", extractor.getSearchSuggestion());
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static YoutubeSearchExtractor extractor(final String filter) throws Exception {
        final SearchQueryHandler query = YoutubeSearchQueryHandlerFactory.getInstance()
                .fromQuery("ambient", List.of(filter), "");
        return new YoutubeSearchExtractor(new YoutubeService(1), query);
    }

    private static JsonObject searchResponse(final JsonArray contents, final String token) {
        final JsonArray sections = new JsonArray(List.of(
                json("itemSectionRenderer", json("contents", contents))));
        if (token != null) {
            sections.add(json("continuationItemRenderer", json("continuationEndpoint",
                    json("continuationCommand", json("token", token)))));
        }
        return json("contents", json("twoColumnSearchResultsRenderer", json("primaryContents",
                json("sectionListRenderer", json("contents", sections)))),
                "padding", "x".repeat(60));
    }

    private static JsonObject continuationResponse(final JsonObject result, final String token) {
        final JsonArray items = new JsonArray(List.of(
                json("itemSectionRenderer", json("contents", new JsonArray(List.of(result)))),
                json("continuationItemRenderer", json("continuationEndpoint",
                        json("continuationCommand", json("token", token))))));
        return json("onResponseReceivedCommands", new JsonArray(List.of(
                json("appendContinuationItemsAction", json("continuationItems", items)))),
                "padding", "x".repeat(60));
    }

    private static JsonObject video(final String id, final String title) {
        return json("videoId", id, "title", json("simpleText", title),
                "lengthText", json("simpleText", "1:23"));
    }

    private static JsonObject channel(final String id, final String name) {
        return json("channelId", id, "title", json("simpleText", name),
                "subscriberCountText", json("simpleText", "1.2K subscribers"));
    }

    private static JsonObject json(final Object... values) {
        final JsonObject object = new JsonObject();
        for (int index = 0; index < values.length; index += 2) {
            object.put((String) values[index], values[index + 1]);
        }
        return object;
    }

    private static JsonObject requestBody(final Request request) throws Exception {
        return JsonParser.object().from(new String(request.dataToSend(), StandardCharsets.UTF_8));
    }

    private static final class StubDownloader extends Downloader {
        private final List<String> responses;
        private final List<Request> requests = new ArrayList<>();
        private int responseIndex;

        private StubDownloader(final List<String> responses) { this.responses = responses; }

        @Override
        public Response execute(final Request request) throws IOException, ReCaptchaException {
            requests.add(request);
            if (request.url().endsWith("/sw.js")) {
                return new Response(200, "OK", Map.of(),
                        "INNERTUBE_CONTEXT_CLIENT_VERSION\":\"2.20261022.01.00\"",
                        request.url());
            }
            if (responseIndex >= responses.size()) {
                throw new IOException("No offline response for " + request.url());
            }
            return new Response(200, "OK", Map.of("Content-Type", List.of("application/json")),
                    responses.get(responseIndex++), request.url());
        }
    }
}
