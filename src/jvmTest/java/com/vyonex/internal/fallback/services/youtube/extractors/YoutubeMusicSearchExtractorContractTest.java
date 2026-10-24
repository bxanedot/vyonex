// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MetaInfo;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.services.youtube.YoutubeService;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class YoutubeMusicSearchExtractorContractTest {
    @Test
    void mapsMusicSearchSuggestionResultsAndContinuationOffline() throws Exception {
        final StubDownloader downloader = new StubDownloader(List.of(
                initialResult("abcdefghijk", "Song"), continuationResult("lmnopqrstuv", "Next")));
        final Downloader previous = VyonEx.getDownloader();
        YoutubeParsingHelper.resetClientVersion();
        VyonEx.init(downloader);
        try {
            final SearchQueryHandler query = YoutubeSearchQueryHandlerFactory.getInstance()
                    .fromQuery("ambient", List.of(MUSIC_SONGS), "");
            final YoutubeMusicSearchExtractor extractor =
                    new YoutubeMusicSearchExtractor(new YoutubeService(11), query);
            extractor.onFetchPage(downloader);

            assertEquals("ambient", extractor.getSearchString());
            assertEquals("ambient waves", extractor.getSearchSuggestion());
            assertFalse(extractor.isCorrectedSearch());
            assertEquals(List.<MetaInfo>of(), extractor.getMetaInfo());

            final var firstPage = extractor.getInitialPage();
            assertEquals(List.of("Song"), firstPage.getItems().stream()
                    .map(InfoItem::getName).toList());
            assertEquals("https://music.youtube.com/watch?v=abcdefghijk",
                    firstPage.getItems().get(0).getUrl());
            assertEquals("Artist", ((com.vyonex.internal.fallback.stream.StreamInfoItem)
                    firstPage.getItems().get(0)).getUploaderName());
            assertEquals("https://music.youtube.com/youtubei/v1/search?ctoken=music-token"
                    + "&continuation=music-token&prettyPrint=false",
                    firstPage.getNextPage().getUrl());

            final var next = extractor.getPage(firstPage.getNextPage());
            assertEquals(List.of("Next"), next.getItems().stream()
                    .map(InfoItem::getName).toList());
            assertEquals("https://music.youtube.com/watch?v=lmnopqrstuv",
                    next.getItems().get(0).getUrl());
            assertEquals(3, downloader.requests.size());
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static String initialResult(final String videoId, final String title) throws Exception {
        final JsonObject suggestion = json("didYouMeanRenderer",
                json("correctedQuery", json("simpleText", "ambient waves")));
        final JsonObject suggestionSection = json("itemSectionRenderer",
                json("contents", new JsonArray(List.of(suggestion))));
        final JsonObject shelf = json("musicShelfRenderer", json(
                "contents", new JsonArray(List.of(songRow(videoId, title))),
                "continuations", new JsonArray(List.of(json("nextContinuationData",
                        json("continuation", "music-token"))))));
        final JsonObject sectionList = json("contents",
                new JsonArray(List.of(suggestionSection, shelf)));
        final JsonObject tab = json("tabRenderer", json("content",
                json("sectionListRenderer", sectionList)));
        return JsonWriter.string(json("contents", json("tabbedSearchResultsRenderer",
                json("tabs", new JsonArray(List.of(tab)))), "padding", "x".repeat(60)));
    }

    private static String continuationResult(final String videoId, final String title)
            throws Exception {
        final JsonObject shelf = json("contents", new JsonArray(List.of(songRow(videoId, title))),
                "continuations", new JsonArray());
        return JsonWriter.string(json("continuationContents",
                json("musicShelfContinuation", shelf), "padding", "x".repeat(60)));
    }

    private static JsonObject songRow(final String videoId, final String title) throws Exception {
        return JsonParser.object().from("""
                {"musicResponsiveListItemRenderer":{
                  "playlistItemData":{"videoId":"%s"},
                  "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"simpleText":"%s"}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[
                      {"text":"Artist","navigationEndpoint":{"browseEndpoint":{"browseId":"UC123"}}},
                      {"text":" • "},{"text":"Album"},{"text":" • "},{"text":"3:05"}
                    ]}}}
                  ],
                  "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[
                    {"url":"//img.example/song.jpg","width":120,"height":120}
                  ]}}}
                }}
                """.formatted(videoId, title));
    }

    private static JsonObject json(final Object... entries) {
        final JsonObject object = new JsonObject();
        for (int index = 0; index < entries.length; index += 2) {
            object.put((String) entries[index], entries[index + 1]);
        }
        return object;
    }

    private static final class StubDownloader extends Downloader {
        private final List<String> responses;
        private final List<Request> requests = new ArrayList<>();
        private int resultIndex;

        private StubDownloader(final List<String> responses) { this.responses = responses; }

        @Override
        public Response execute(final Request request) throws IOException {
            requests.add(request);
            final String body = request.url().contains("get_search_suggestions")
                    ? "x".repeat(501) : resultIndex < responses.size()
                    ? responses.get(resultIndex++) : null;
            if (body == null) throw new IOException("No music search fixture remains");
            return new Response(200, "OK", Map.of(), body, request.url());
        }
    }
}
