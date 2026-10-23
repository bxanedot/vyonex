// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudChannelExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudCommentsExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudPlaylistExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudStreamExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudSuggestionExtractor;
import com.vyonex.internal.fallback.stream.AudioStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundcloudServiceExtractionTest {
    private static final String HOME = "https://soundcloud.com";
    private static final String SCRIPT = "https://cdn.sndcdn.com/assets/client.js";
    private static final String CLIENT_ID = "offline-key";

    @Test
    void mapsChannelProfileFromOfflineApiResponse() throws Exception {
        final String profileUrl = "https://api-v2.soundcloud.com/users/7?client_id=" + CLIENT_ID;
        final StubDownloader downloader = new StubDownloader(Map.of(
                HOME, "<script src=\"" + SCRIPT + "\"></script>",
                SCRIPT, "client_id:\"" + CLIENT_ID + "\"",
                profileUrl, """
                        {"username":"Alice","avatar_url":"https://images.example/alice.jpg",
                         "followers_count":12,"description":"Producer","verified":true,
                         "visuals":{"visuals":[{"visual_url":"https://images.example/banner.jpg"}]}}
                        """));
        final Downloader previous = VyonEx.getDownloader();
        try {
            VyonEx.init(downloader);
            final String channelUrl = "https://soundcloud.com/alice";
            final ListLinkHandler handler = new ListLinkHandler(
                    channelUrl, channelUrl, "7", List.of(), "");
            final SoundcloudChannelExtractor extractor = new SoundcloudChannelExtractor(
                    new SoundcloudService(31), handler);
            extractor.onFetchPage(downloader);

            assertEquals("7", extractor.getId());
            assertEquals("Alice", extractor.getName());
            assertEquals("https://images.example/alice.jpg", extractor.getAvatars().get(0).getUrl());
            assertEquals("https://images.example/banner.jpg", extractor.getBanners().get(0).getUrl());
            assertEquals(12, extractor.getSubscriberCount());
            assertEquals("Producer", extractor.getDescription());
            assertTrue(extractor.isVerified());
            assertEquals(4, extractor.getTabs().size());
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void readsSuggestionsAndSearchPagesFromOfflineApiResponses() throws Exception {
        final String searchUrl = "https://api-v2.soundcloud.com/search?q=ambient&client_id="
                + CLIENT_ID + "&limit=10&offset=0";
        final String search = """
                {"collection":[{"kind":"track","id":42,"title":"Rain","duration":90000,
                 "permalink_url":"https://soundcloud.com/a/rain","created_at":"2026-10-22T12:30:00Z",
                 "user":{"username":"Alice","permalink_url":"https://soundcloud.com/alice",
                   "avatar_url":"https://images.example/alice.jpg"},"artwork_url":"https://images.example/rain.jpg"}],
                 "next_href":"https://api-v2.soundcloud.com/search?q=ambient&offset=10"}
                """;
        final StubDownloader downloader = new StubDownloader(Map.of(
                HOME, "<script src=\"" + SCRIPT + "\"></script>",
                SCRIPT, "client_id:\"" + CLIENT_ID + "\"",
                searchUrl, search,
                "https://api-v2.soundcloud.com/search/queries?q=ambient&client_id="
                        + CLIENT_ID + "&limit=10",
                "{\"collection\":[{\"query\":\"ambient music\"}]}"));
        final Downloader previous = VyonEx.getDownloader();
        try {
            VyonEx.init(downloader);
            final StreamingService service = new SoundcloudService(31);
            final SearchQueryHandler query = new SearchQueryHandler(searchUrl, searchUrl,
                    "ambient", List.of("all"), "");
            final SearchExtractor extractor = service.getSearchExtractor(query);
            extractor.onFetchPage(downloader);
            final var firstPage = extractor.getInitialPage();

            assertEquals(1, firstPage.getItems().size());
            assertEquals("Rain", firstPage.getItems().get(0).getName());
            assertTrue(firstPage.getNextPage().getUrl().contains("offset=10"));
            assertEquals(List.of("ambient music"),
                    new SoundcloudSuggestionExtractor(service).suggestionList("ambient"));
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void resolvesProgressiveAudioStreamsFromTheApi() throws Exception {
        final String trackUrl = "https://soundcloud.com/a/song";
        final String resolveUrl = "https://api-v2.soundcloud.com/resolve?url="
                + "https%3A%2F%2Fsoundcloud.com%2Fa%2Fsong&client_id=" + CLIENT_ID;
        final String transcode = "https://api-v2.soundcloud.com/media/track/42/stream"
                + "?client_id=" + CLIENT_ID;
        final StubDownloader downloader = new StubDownloader(Map.of(
                HOME, "<script src=\"" + SCRIPT + "\"></script>",
                SCRIPT, "client_id:\"" + CLIENT_ID + "\"",
                resolveUrl, """
                        {"id":42,"policy":"ALLOW","streamable":true,
                         "media":{"transcodings":[{"url":"https://api-v2.soundcloud.com/media/track/42/stream",
                           "preset":"mp3_0_0","format":{"protocol":"progressive"}}]}}
                        """,
                transcode, "{\"url\":\"https://audio.example/song.mp3\"}"));
        final Downloader previous = VyonEx.getDownloader();
        try {
            VyonEx.init(downloader);
            final SoundcloudStreamExtractor extractor = new SoundcloudStreamExtractor(
                    new SoundcloudService(31), new LinkHandler(trackUrl, trackUrl, "42"));
            extractor.onFetchPage(downloader);
            final List<AudioStream> streams = extractor.getAudioStreams();
            assertEquals(1, streams.size());
            assertEquals("https://audio.example/song.mp3", streams.get(0).getContent());
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void parsesCommentPagesWithoutNetworkAccess() throws Exception {
        final String commentsUrl = "https://api-v2.soundcloud.com/tracks/42/comments?client_id="
                + CLIENT_ID + "&threaded=0&filter_replies=1";
        final StubDownloader downloader = new StubDownloader(Map.of(commentsUrl, """
                {"collection":[{"id":5,"body":"Great","timestamp":3500,
                 "created_at":"2026-10-22T12:30:00Z","user":{"username":"Alice"}}],
                 "next_href":"https://api-v2.soundcloud.com/tracks/42/comments?offset=10"}
                """));
        final Downloader previous = VyonEx.getDownloader();
        try {
            VyonEx.init(downloader);
            final var handler = new com.vyonex.internal.fallback.linkhandler.ListLinkHandler(
                    commentsUrl, commentsUrl, "42", List.of(), "");
            final SoundcloudCommentsExtractor extractor = new SoundcloudCommentsExtractor(
                    new SoundcloudService(31), handler);
            final var page = extractor.getInitialPage();
            assertEquals(1, page.getItems().size());
            assertEquals("5", page.getItems().get(0).getCommentId());
            assertEquals("Great", page.getItems().get(0).getCommentText().getContent());
            assertTrue(page.getNextPage().getUrl().contains("offset=10"));
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void restoresPlaylistTrackOrderAfterBatchLookup() throws Exception {
        final String playlistUrl = "https://api-v2.soundcloud.com/playlists/7?client_id="
                + CLIENT_ID + "&representation=compact";
        final String tracksUrl = "https://api-v2.soundcloud.com/tracks?client_id="
                + CLIENT_ID + "&ids=2,1";
        final StubDownloader downloader = new StubDownloader(Map.of(
                HOME, "<script src=\"" + SCRIPT + "\"></script>",
                SCRIPT, "client_id:\"" + CLIENT_ID + "\"",
                playlistUrl, """
                        {"id":7,"title":"Mix","track_count":2,"tracks":[{"id":2},{"id":1}],
                         "user":{"username":"Alice"}}
                        """,
                tracksUrl, """
                        [{"id":1,"title":"First","duration":1000},
                         {"id":2,"title":"Second","duration":1000}]
                        """));
        final Downloader previous = VyonEx.getDownloader();
        try {
            VyonEx.init(downloader);
            final var handler = new com.vyonex.internal.fallback.linkhandler.ListLinkHandler(
                    "https://soundcloud.com/alice/sets/mix", "https://soundcloud.com/alice/sets/mix",
                    "7", List.of(), "");
            final SoundcloudPlaylistExtractor extractor = new SoundcloudPlaylistExtractor(
                    new SoundcloudService(31), handler);
            extractor.onFetchPage(downloader);
            final var firstPage = extractor.getInitialPage();
            final var tracks = extractor.getPage(firstPage.getNextPage());

            assertEquals("Second", tracks.getItems().get(0).getName());
            assertEquals("First", tracks.getItems().get(1).getName());
            assertNull(tracks.getNextPage());
        } finally {
            VyonEx.init(previous);
        }
    }

    private static final class StubDownloader extends Downloader {
        private final Map<String, String> responses;

        private StubDownloader(final Map<String, String> responses) { this.responses = responses; }

        @Override public Response execute(final Request request)
                throws IOException, ReCaptchaException {
            final String body = responses.get(request.url());
            if (body == null) throw new IOException("No offline response for " + request.url());
            return new Response(200, "OK", Map.of(), body, request.url());
        }
    }
}
