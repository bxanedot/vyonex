// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.comments.CommentsExtractor;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeCommentsExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PeertubeCommentsExtractorTest {
    private Downloader previousDownloader;
    private Localization previousLocalization;
    private ContentCountry previousContentCountry;

    @BeforeEach
    void rememberVyonExState() {
        previousDownloader = VyonEx.getDownloader();
        previousLocalization = VyonEx.getPreferredLocalization();
        previousContentCountry = VyonEx.getPreferredContentCountry();
    }

    @AfterEach
    void restoreVyonExState() {
        VyonEx.init(previousDownloader, previousLocalization, previousContentCountry);
    }

    @Test
    void mapsCommentsSkipsDeletedEntriesAndCarriesNestedRepliesOffline() throws Exception {
        final String commentsUrl = "https://tube.example/api/v1/videos/video-1/comment-threads";
        final String threadUrl = commentsUrl + "/101";
        final String topLevel = """
                {"total":13,"data":[
                  {"id":101,"threadId":101,"text":"<p>Hello <strong>world</strong></p>",
                   "createdAt":"2026-01-02T03:04:05Z","isDeleted":false,
                   "totalReplies":2,"totalRepliesFromVideoAuthor":1,
                   "account":{"displayName":"Alice","name":"alice","host":"federated.example",
                     "avatar":{"path":"/alice.png"}}},
                  {"id":102,"isDeleted":true}]}
                """;
        final String thread = """
                {"children":[{"comment":{"id":103,"threadId":101,"text":"Reply",
                   "createdAt":"2026-01-03T03:04:05Z","isDeleted":false,"totalReplies":0,
                   "account":{"displayName":"Bob","name":"bob","host":"tube.example"}},
                   "children":[{"comment":{"id":104,"threadId":101,"text":"Nested",
                   "createdAt":"2026-01-04T03:04:05Z","isDeleted":false,
                   "account":{"displayName":"Carol","name":"carol","host":"tube.example"}},
                   "children":[]}]}]}
                """;
        final StubDownloader downloader = new StubDownloader(Map.of(
                commentsUrl + "?start=0&count=12", topLevel, threadUrl, thread));
        VyonEx.init(downloader);
        final PeertubeService service = new PeertubeService(3,
                new PeertubeInstance("https://tube.example", "Offline"));
        final ListLinkHandler handler = service.getCommentsLHFactory()
                .fromUrl("https://tube.example/videos/watch/video-1");
        final CommentsExtractor extractor = service.getCommentsExtractor(handler);

        final var firstPage = extractor.getInitialPage();

        assertEquals(1, firstPage.getItems().size());
        assertEquals("Hello world", firstPage.getItems().get(0).getCommentText().getContent());
        assertEquals(Instant.parse("2026-01-02T03:04:05Z"),
                firstPage.getItems().get(0).getUploadDate().getInstant());
        assertEquals("alice@federated.example", firstPage.getItems().get(0).getUploaderName());
        assertEquals("https://tube.example/accounts/alice@federated.example",
                firstPage.getItems().get(0).getUploaderUrl());
        assertEquals("https://tube.example/alice.png",
                firstPage.getItems().get(0).getUploaderAvatars().get(0).getUrl());
        assertTrue(firstPage.getItems().get(0).hasCreatorReply());
        assertEquals(2, firstPage.getItems().get(0).getReplyCount());
        assertNotNull(firstPage.getNextPage());
        assertEquals(commentsUrl + "?start=12&count=12", firstPage.getNextPage().getUrl());

        final var replies = extractor.getPage(firstPage.getItems().get(0).getReplies());

        assertEquals(1, replies.getItems().size());
        assertEquals("103", replies.getItems().get(0).getCommentId());
        assertEquals(1, replies.getItems().get(0).getReplyCount());
        assertNotNull(replies.getItems().get(0).getReplies().getBody());

        final var nested = extractor.getPage(replies.getItems().get(0).getReplies());
        assertEquals(1, nested.getItems().size());
        assertEquals("104", nested.getItems().get(0).getCommentId());
        assertEquals(0, nested.getItems().get(0).getReplyCount());
        assertNull(nested.getNextPage());
        assertEquals(List.of(commentsUrl + "?start=0&count=12", threadUrl),
                downloader.requestedUrls);
    }

    private static final class StubDownloader extends Downloader {
        private final Map<String, String> bodies;
        private final List<String> requestedUrls = new ArrayList<>();

        private StubDownloader(final Map<String, String> bodies) { this.bodies = bodies; }

        @Override public Response execute(final Request request)
                throws IOException, ReCaptchaException {
            requestedUrls.add(request.url());
            final String body = bodies.get(request.url());
            if (body == null) throw new IOException("No offline response for " + request.url());
            return new Response(200, "OK", Map.of(), body, request.url());
        }
    }
}
