// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.comments.CommentsInfoItem;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.services.youtube.YoutubeService;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeCommentsLinkHandlerFactory;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeCommentsExtractorContractTest {
    private static final String INITIAL = """
            {"contents":{"twoColumnWatchNextResults":{"results":{"results":{"contents":[
              {"itemSectionRenderer":{"targetId":"comments-section","contents":[
                {"continuationItemRenderer":{"continuationEndpoint":{"continuationCommand":{"token":"first-token"}}}}
              ]}}
            ]}}}}}
            """;
    private static final String FIRST_PAGE = """
            {"onResponseReceivedEndpoints":[{"reloadContinuationItemsCommand":{"continuationItems":[
              {"commentsHeaderRenderer":{"countText":{"simpleText":"2 comments"}}},
              {"commentThreadRenderer":{"comment":{"commentRenderer":{
                "commentId":"comment-1","authorText":{"simpleText":"Alice"},
                "contentText":{"simpleText":"Hello"},"publishedTimeText":{"simpleText":"1 day ago"},
                "voteCount":{"simpleText":"1"}
              }},"replies":{"commentRepliesRenderer":{"contents":[]}}}},
              {"continuationItemRenderer":{"continuationEndpoint":{"continuationCommand":{"token":"second-token"}}}}
            ]}}],"frameworkUpdates":{"entityBatchUpdate":{"mutations":[]}}}
            """;
    private static final String SECOND_PAGE = """
            {"onResponseReceivedEndpoints":[{"appendContinuationItemsAction":{"continuationItems":[
              {"commentRenderer":{"commentId":"comment-2","authorText":{"simpleText":"Bob"},
                "contentText":{"simpleText":"Thanks"},"publishedTimeText":{"simpleText":"1 hour ago"},
                "voteCount":{"simpleText":"0"}}}
            ]}}],"frameworkUpdates":{"entityBatchUpdate":{"mutations":[]}}}
            """;

    @Test
    void fetchesCommentsMapsItemsAndFollowsContinuation() throws Exception {
        final StubDownloader downloader = new StubDownloader(List.of(INITIAL, FIRST_PAGE, SECOND_PAGE));
        final Downloader previous = VyonEx.getDownloader();
        YoutubeParsingHelper.resetClientVersion();
        VyonEx.init(downloader);
        try {
            final var link = YoutubeCommentsLinkHandlerFactory.getInstance()
                    .fromUrl("https://www.youtube.com/watch?v=abcdefghijk");
            final var extractor = new YoutubeCommentsExtractor(new YoutubeService(7), link);
            extractor.fetchPage();

            assertFalse(extractor.isCommentsDisabled());
            assertEquals(2, extractor.getCommentsCount());
            final var first = extractor.getInitialPage();
            assertEquals(List.of("Alice"), first.getItems().stream()
                    .map(CommentsInfoItem::getName).toList());
            assertEquals("comment-1", first.getItems().get(0).getCommentId());
            assertEquals("abcdefghijk", body(downloader.requests.get(1)).getString("videoId"));
            assertEquals("first-token", body(downloader.requests.get(2)).getString("continuation"));
            assertEquals("second-token", first.getNextPage().getId());

            final var second = extractor.getPage(first.getNextPage());
            assertEquals(List.of("Bob"), second.getItems().stream()
                    .map(CommentsInfoItem::getName).toList());
            assertNull(second.getNextPage());
            assertEquals("second-token", body(downloader.requests.get(3)).getString("continuation"));
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    @Test
    void reportsDisabledCommentsWithoutRequestingACommentPage() throws Exception {
        final StubDownloader downloader = new StubDownloader(List.of(
                "{\"contents\":{},\"padding\":\"" + "x".repeat(60) + "\"}"));
        final Downloader previous = VyonEx.getDownloader();
        YoutubeParsingHelper.resetClientVersion();
        VyonEx.init(downloader);
        try {
            final var link = YoutubeCommentsLinkHandlerFactory.getInstance()
                    .fromUrl("https://www.youtube.com/watch?v=abcdefghijk");
            final var extractor = new YoutubeCommentsExtractor(new YoutubeService(7), link);
            extractor.fetchPage();

            assertTrue(extractor.isCommentsDisabled());
            assertEquals(-1, extractor.getCommentsCount());
            assertTrue(extractor.getInitialPage().getItems().isEmpty());
            assertNull(extractor.getInitialPage().getNextPage());
            assertEquals(2, downloader.requests.size());
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static JsonObject body(final Request request) throws Exception {
        return JsonParser.object().from(new String(request.dataToSend(), StandardCharsets.UTF_8));
    }

    private static final class StubDownloader extends Downloader {
        private final Queue<String> responses;
        private final List<Request> requests = new ArrayList<>();

        private StubDownloader(final List<String> responses) {
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public Response execute(final Request request) throws IOException {
            requests.add(request);
            if (request.url().endsWith("/sw.js")) {
                return new Response(200, "OK", Map.of(),
                        "INNERTUBE_CONTEXT_CLIENT_VERSION\":\"2.20261022.01.00\"",
                        request.url());
            }
            final String response = responses.poll();
            if (response == null) throw new IOException("No offline response remains");
            return new Response(200, "OK", Map.of("Content-Type", List.of("application/json")),
                    response, request.url());
        }
    }
}
