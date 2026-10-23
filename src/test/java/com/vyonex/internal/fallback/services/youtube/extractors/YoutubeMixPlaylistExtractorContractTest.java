// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.playlist.PlaylistInfo;
import com.vyonex.internal.fallback.services.youtube.YoutubeService;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeMixPlaylistExtractorContractTest {
    private static final String PLAYLIST_ID = "RDMMabcdefghijk";
    private static final String INITIAL = """
            {"contents":{"twoColumnWatchNextResults":{"playlist":{"playlist":{
              "title":{"simpleText":"Daily Mix"},"playlistId":"RDMMabcdefghijk",
              "contents":[{"playlistPanelVideoRenderer":{
                "videoId":"abcdefghijk","title":{"simpleText":"Current"},
                "lengthText":{"simpleText":"3:00"},"ownerText":{"simpleText":"Artist"},
                "thumbnail":{"thumbnails":[{"url":"//img.example/current.jpg","width":480,"height":360}]},
                "navigationEndpoint":{"watchEndpoint":{"playlistId":"RDMMabcdefghijk","videoId":"abcdefghijk","index":0,"params":"mix-params"}}
              }}]
            }}}},"currentVideoEndpoint":{"watchEndpoint":{"videoId":"abcdefghijk"}}}
            """;
    private static final String CONTINUATION = """
            {"contents":{"twoColumnWatchNextResults":{"playlist":{"playlist":{
              "playlistId":"RDMMabcdefghijk","currentIndex":0,
              "contents":[
                {"playlistPanelVideoRenderer":{"videoId":"abcdefghijk","title":{"simpleText":"Current"},"navigationEndpoint":{"watchEndpoint":{"playlistId":"RDMMabcdefghijk","videoId":"abcdefghijk","index":0,"params":"mix-params"}}}},
                {"playlistPanelVideoRenderer":{"videoId":"lmnopqrstuv","title":{"simpleText":"Next"},"lengthText":{"simpleText":"2:00"},"navigationEndpoint":{"watchEndpoint":{"playlistId":"RDMMabcdefghijk","videoId":"lmnopqrstuv","index":1,"params":"mix-params"}}}}
              ]
            }}}}}
            """;

    @Test
    void mapsMixAndPaginatesWithVisitorCookie() throws Exception {
        final StubDownloader downloader = new StubDownloader(List.of(INITIAL, CONTINUATION));
        final Downloader previous = VyonEx.getDownloader();
        YoutubeParsingHelper.resetClientVersion();
        VyonEx.init(downloader);
        try {
            final ListLinkHandler link = YoutubePlaylistLinkHandlerFactory.getInstance()
                    .fromUrl("https://www.youtube.com/watch?v=abcdefghijk&list=" + PLAYLIST_ID);
            final YoutubeMixPlaylistExtractor extractor =
                    new YoutubeMixPlaylistExtractor(new YoutubeService(7), link);
            extractor.onFetchPage(downloader);

            assertEquals("Daily Mix", extractor.getName());
            assertEquals("YouTube", extractor.getUploaderName());
            assertEquals("", extractor.getUploaderUrl());
            assertEquals(PlaylistInfo.PlaylistType.MIX_STREAM, extractor.getPlaylistType());
            assertEquals(3, extractor.getThumbnails().size());

            final var firstPage = extractor.getInitialPage();
            assertEquals(List.of("Current"), firstPage.getItems().stream()
                    .map(StreamInfoItem::getName).toList());
            assertNotNull(firstPage.getNextPage());
            assertFalse(firstPage.getNextPage().getCookies().isEmpty());

            final var next = extractor.getPage(firstPage.getNextPage());
            assertEquals(List.of("Next"), next.getItems().stream()
                    .map(StreamInfoItem::getName).toList());
            assertNotNull(next.getNextPage());
            assertEquals(3, downloader.requests.size());
            assertTrue(downloader.requests.get(2).headers().get("Cookie").get(0)
                    .contains("VISITOR_INFO1_LIVE=visitor"));
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    @Test
    void emptyMixHasNoContinuation() throws Exception {
        final String emptyMix = """
                {"contents":{"twoColumnWatchNextResults":{"playlist":{"playlist":{
                  "title":{"simpleText":"Empty Mix"},"playlistId":"RDMMabcdefghijk","contents":[]
                }}}},"currentVideoEndpoint":{"watchEndpoint":{"videoId":"abcdefghijk"}}}
                """;
        final StubDownloader downloader = new StubDownloader(List.of(emptyMix));
        final Downloader previous = VyonEx.getDownloader();
        YoutubeParsingHelper.resetClientVersion();
        VyonEx.init(downloader);
        try {
            final var link = YoutubePlaylistLinkHandlerFactory.getInstance().fromUrl(
                    "https://www.youtube.com/watch?v=abcdefghijk&list=" + PLAYLIST_ID);
            final var extractor = new YoutubeMixPlaylistExtractor(new YoutubeService(7), link);
            extractor.onFetchPage(downloader);

            final var page = extractor.getInitialPage();
            assertTrue(page.getItems().isEmpty());
            assertNull(page.getNextPage());
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static final class StubDownloader extends Downloader {
        private final Queue<String> responses;
        private final List<Request> requests = new java.util.ArrayList<>();

        private StubDownloader(final List<String> responses) {
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public Response execute(final Request request) throws IOException {
            requests.add(request);
            if (request.url().equals("https://www.youtube.com/sw.js")) {
                return new Response(200, "OK", Map.of(),
                        "INNERTUBE_CONTEXT_CLIENT_VERSION\":\"2.20261022.01.00\"",
                        request.url());
            }
            final String body = responses.poll();
            if (body == null) throw new IOException("No response fixture remains");
            return new Response(200, "OK",
                    Map.of("Set-Cookie", List.of("VISITOR_INFO1_LIVE=visitor; Path=/")),
                    body, "https://www.youtube.com/youtubei/v1/next");
        }
    }
}
