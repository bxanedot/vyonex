// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.services.youtube.YoutubeService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class YoutubeChannelTabExtractorContractTest {
    @Test
    void requestsSelectedChannelTabAndMapsGridVideo() throws Exception {
        final String channel = """
                {"metadata":{"channelMetadataRenderer":{"externalChannelId":"UC123","title":"Channel"}},
                 "contents":{"twoColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{
                   "endpoint":{"commandMetadata":{"webCommandMetadata":{"url":"/@channel/videos"}}},
                   "content":{"sectionListRenderer":{"contents":[{"itemSectionRenderer":{"contents":[
                     {"gridRenderer":{"items":[{"gridVideoRenderer":{"videoId":"abcdefghijk",
                       "title":{"simpleText":"Channel video"},"lengthText":{"simpleText":"2:00"}}}]}}
                   ]}}]}}
                 }}]}},"padding":"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"}
                """;
        final StubDownloader downloader = new StubDownloader(List.of(channel));
        final Downloader previous = VyonEx.getDownloader();
        YoutubeParsingHelper.resetClientVersion();
        VyonEx.init(downloader);
        try {
            final String url = "https://www.youtube.com/channel/UC123/videos";
            final var link = new ListLinkHandler(url, url, "UC123",
                    List.of(ChannelTabs.VIDEOS), "");
            final var extractor = new YoutubeChannelTabExtractor(new YoutubeService(7), link);
            extractor.fetchPage();

            assertEquals("UC123", extractor.getId());
            assertEquals("Channel", extractor.getChannelName());
            assertEquals("https://www.youtube.com/channel/UC123/videos", extractor.getUrl());
            assertEquals(List.of("Channel video"), names(extractor.getInitialPage().getItems()));
            assertEquals("UC123", requestBody(downloader.requests.get(1)).getString("browseId"));
            assertEquals("EgZ2aWRlb3PyBgQKAjoA",
                    requestBody(downloader.requests.get(1)).getString("params"));
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    @Test
    void carriesChannelIdentityThroughContinuationPages() throws Exception {
        final String tab = """
                {"content":{"sectionListRenderer":{"contents":[{"itemSectionRenderer":{"contents":[
                  {"gridRenderer":{"items":[
                    {"gridVideoRenderer":{"videoId":"abcdefghijk","title":{"simpleText":"First"},"lengthText":{"simpleText":"1:00"}}},
                    {"continuationItemRenderer":{"continuationEndpoint":{"continuationCommand":{"token":"next-token"}}}}
                  ]}}
                ]}}]}}}
                """;
        final String next = """
                {"onResponseReceivedActions":[{"appendContinuationItemsAction":{"continuationItems":[
                  {"gridVideoRenderer":{"videoId":"lmnopqrstuv","title":{"simpleText":"Next"},"lengthText":{"simpleText":"2:00"}}}
                ]}}],"padding":"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"}
                """;
        final StubDownloader downloader = new StubDownloader(List.of(next));
        final Downloader previous = VyonEx.getDownloader();
        YoutubeParsingHelper.resetClientVersion();
        VyonEx.init(downloader);
        try {
            final String url = "https://www.youtube.com/channel/UC123/videos";
            final ListLinkHandler link = new ListLinkHandler(url, url, "UC123",
                    List.of(ChannelTabs.VIDEOS), "");
            final var extractor = new YoutubeChannelTabExtractor.VideosTabExtractor(
                    new YoutubeService(7), link, JsonParser.object().from(tab), null,
                    "Channel", "UC123", "https://www.youtube.com/channel/UC123");
            final var first = extractor.getInitialPage();

            assertEquals(List.of("First"), names(first.getItems()));
            final Page page = first.getNextPage();
            assertNotNull(page);
            assertEquals("next-token", requestBody(page.getBody()).getString("continuation"));
            final var second = extractor.getPage(page);
            final StreamInfoItem item = (StreamInfoItem) second.getItems().get(0);
            assertEquals("Next", item.getName());
            assertEquals("Channel", item.getUploaderName());
            assertEquals("https://www.youtube.com/channel/UC123", item.getUploaderUrl());
            assertNull(second.getNextPage());
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static List<String> names(final List<? extends InfoItem> items) {
        return items.stream().map(InfoItem::getName).toList();
    }

    private static JsonObject requestBody(final Request request) throws Exception {
        return requestBody(request.dataToSend());
    }

    private static JsonObject requestBody(final byte[] body) throws Exception {
        return JsonParser.object().from(new String(body, StandardCharsets.UTF_8));
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
