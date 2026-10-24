// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.playlist.PlaylistInfo;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.services.youtube.YoutubeService;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubePlaylistExtractorContractTest {
    private static final String SIDEBAR_METADATA = """
            {"sidebar":{"playlistSidebarRenderer":{"items":[
              {"playlistSidebarPrimaryInfoRenderer":{"title":{"simpleText":"Road trip"},
                "description":{"simpleText":"A playlist"},"stats":[{"simpleText":"2 videos"}],
                "thumbnailRenderer":{"playlistVideoThumbnailRenderer":{"thumbnail":{"thumbnails":[
                  {"url":"https://img.example/playlist.jpg","width":480,"height":360}
                ]}}}}},
              {"playlistSidebarSecondaryInfoRenderer":{"videoOwner":{"videoOwnerRenderer":{
                "title":{"simpleText":"Owner"},"navigationEndpoint":{"browseEndpoint":{"browseId":"UC123"}},
                "thumbnail":{"thumbnails":[{"url":"https://img.example/owner.jpg","width":48,"height":48}]}
              }}}}
            ]}},"padding":"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"}
            """;
    private static final String HEADER_METADATA = """
            {"header":{"playlistHeaderRenderer":{"title":{"simpleText":"New layout"},
              "ownerText":{"runs":[{"text":"Owner","navigationEndpoint":{"browseEndpoint":{"browseId":"UC123"}}}]},
              "numVideosText":{"simpleText":"5 videos"},"descriptionText":{"simpleText":"Header description"},
              "playlistHeaderBanner":{"heroPlaylistThumbnailRenderer":{"thumbnail":{"thumbnails":[
                {"url":"https://img.example/banner.jpg","width":480,"height":360}
              ]}}}}},"padding":"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"}
            """;
    private static final String INITIAL_ITEMS = """
            {"onResponseReceivedActions":[{"reloadContinuationItemsCommand":{"continuationItems":[
              {"playlistVideoRenderer":{"videoId":"abcdefghijk","title":{"simpleText":"First"},
                "shortBylineText":{"runs":[{"text":"Owner"}]},"lengthText":{"simpleText":"3:00"},
                "thumbnail":{"thumbnails":[{"url":"https://img.example/video.jpg","width":480,"height":360}]}}},
              {"continuationItemRenderer":{"continuationEndpoint":{"commandExecutorCommand":{"commands":[
                {"continuationCommand":{"token":"next-token"}}
              ]}}}}
            ]}}],"padding":"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"}
            """;
    private static final String NEXT_ITEMS = """
            {"onResponseReceivedActions":[{"appendContinuationItemsAction":{"continuationItems":[
              {"playlistVideoRenderer":{"videoId":"lmnopqrstuv","title":{"simpleText":"Second"},
                "shortBylineText":{"runs":[{"text":"Owner"}]},"lengthText":{"simpleText":"2:00"}}}
            ]}}],"padding":"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"}
            """;

    @Test
    void mapsSidebarPlaylistAndContinuesOffline() throws Exception {
        final StubDownloader downloader = new StubDownloader(
                List.of(SIDEBAR_METADATA, INITIAL_ITEMS, NEXT_ITEMS));
        final Downloader previous = VyonEx.getDownloader();
        YoutubeParsingHelper.resetClientVersion();
        VyonEx.init(downloader);
        try {
            final var extractor = extractor("https://www.youtube.com/playlist?list=PL1234567890");
            extractor.fetchPage();

            assertEquals("Road trip", extractor.getName());
            assertEquals("A playlist", extractor.getDescription().getContent());
            assertEquals(2, extractor.getStreamCount());
            assertEquals("Owner", extractor.getUploaderName());
            assertEquals("https://www.youtube.com/channel/UC123", extractor.getUploaderUrl());
            assertEquals(PlaylistInfo.PlaylistType.NORMAL, extractor.getPlaylistType());
            assertFalse(extractor.getThumbnails().isEmpty());
            assertFalse(extractor.getUploaderAvatars().isEmpty());

            final var first = extractor.getInitialPage();
            assertEquals(List.of("First"), names(first.getItems()));
            assertNotNull(first.getNextPage());
            assertEquals("next-token", body(first.getNextPage().getBody()).getString("continuation"));
            assertEquals(List.of("Second"), names(extractor.getPage(first.getNextPage()).getItems()));
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    @Test
    void readsMetadataFromHeaderOnlyLayout() throws Exception {
        final StubDownloader downloader = new StubDownloader(List.of(HEADER_METADATA,
                "{\"padding\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}"));
        final Downloader previous = VyonEx.getDownloader();
        YoutubeParsingHelper.resetClientVersion();
        VyonEx.init(downloader);
        try {
            final var extractor = extractor("https://www.youtube.com/playlist?list=PL1234567890");
            extractor.fetchPage();

            assertEquals("New layout", extractor.getName());
            assertEquals("Header description", extractor.getDescription().getContent());
            assertEquals(5, extractor.getStreamCount());
            assertEquals("Owner", extractor.getUploaderName());
            assertEquals("https://www.youtube.com/channel/UC123", extractor.getUploaderUrl());
            assertTrue(extractor.getUploaderAvatars().isEmpty());
            assertEquals("https://img.example/banner.jpg", extractor.getThumbnails().get(0).getUrl());
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static YoutubePlaylistExtractor extractor(final String url) throws Exception {
        final var link = YoutubePlaylistLinkHandlerFactory.getInstance().fromUrl(url);
        return new YoutubePlaylistExtractor(new YoutubeService(7), link);
    }

    private static List<String> names(final List<StreamInfoItem> items) {
        return items.stream().map(StreamInfoItem::getName).toList();
    }

    private static JsonObject body(final byte[] data) throws Exception {
        return JsonParser.object().from(new String(data, StandardCharsets.UTF_8));
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
