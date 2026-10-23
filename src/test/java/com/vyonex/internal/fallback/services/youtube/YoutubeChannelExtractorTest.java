// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeChannelExtractor;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeChannelExtractorTest {
    @Test
    void readsChannelMetadataAndTabsOffline() throws Exception {
        final JsonObject header = json("c4TabbedHeaderRenderer", json(
                "channelId", "UC123", "title", "Creator",
                "subscriberCountText", json("simpleText", "1.2M subscribers"),
                "badges", new JsonArray(List.of(json("metadataBadgeRenderer",
                        json("style", "BADGE_STYLE_TYPE_VERIFIED")))),
                "avatar", json("thumbnails", new JsonArray(List.of(
                        json("url", "//img.example/avatar.jpg", "height", 100, "width", 100)))),
                "banner", json("thumbnails", new JsonArray(List.of(
                        json("url", "//img.example/banner.jpg", "height", 100, "width", 300))))));
        final JsonObject response = normalChannel(header);
        final StubDownloader downloader = new StubDownloader(response);
        final Downloader previous = VyonEx.getDownloader();
        try {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(downloader);
            final YoutubeChannelExtractor channel = channel("UC123");
            channel.fetchPage();

            assertEquals("UC123", channel.getId());
            assertEquals("Creator", channel.getName());
            assertEquals("https://www.youtube.com/channel/UC123", channel.getUrl());
            assertEquals("https://www.youtube.com/feeds/videos.xml?channel_id=UC123",
                    channel.getFeedUrl());
            assertEquals(1_200_000, channel.getSubscriberCount());
            assertEquals("Channel description", channel.getDescription());
            assertTrue(channel.isVerified());
            assertEquals(List.of("music", "live"), channel.getTags());
            assertEquals("https://img.example/avatar.jpg", channel.getAvatars().get(0).getUrl());
            assertEquals("https://img.example/banner.jpg", channel.getBanners().get(0).getUrl());
            final List<String> tabs = channel.getTabs().stream().map(tab -> tab.getUrl()).toList();
            assertTrue(tabs.get(0).endsWith("/videos"));
            assertTrue(tabs.stream().anyMatch(url -> url.endsWith("/shorts")));
            assertTrue(tabs.stream().anyMatch(url -> url.endsWith("/streams")));
            assertTrue(tabs.stream().anyMatch(url -> url.endsWith("/releases")));
            assertTrue(tabs.stream().anyMatch(url -> url.endsWith("/playlists")));
            assertEquals("", channel.getParentChannelName());
            assertEquals("", channel.getParentChannelUrl());
            assertTrue(channel.getParentChannelAvatars().isEmpty());
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    @Test
    void suppliesLimitedMetadataForAgeGatedChannelOffline() throws Exception {
        final JsonObject ageGate = json("channelTitle", "Age-gated",
                "avatar", json("thumbnails", new JsonArray(List.of(
                        json("url", "//img.example/age.jpg", "height", 100, "width", 100)))));
        final JsonObject response = json("contents", json("twoColumnBrowseResultsRenderer",
                json("tabs", new JsonArray(List.of(json("tabRenderer", json("content",
                        json("sectionListRenderer", json("contents", new JsonArray(List.of(
                                json("channelAgeGateRenderer", ageGate))))))))))),
                "padding", "x".repeat(60));
        final StubDownloader downloader = new StubDownloader(response);
        final Downloader previous = VyonEx.getDownloader();
        try {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(downloader);
            final YoutubeChannelExtractor channel = channel("UC789");
            channel.fetchPage();

            assertEquals("Age-gated", channel.getName());
            assertFalse(channel.isVerified());
            assertEquals(-1, channel.getSubscriberCount());
            assertNull(channel.getDescription());
            assertTrue(channel.getBanners().isEmpty());
            assertTrue(channel.getTags().isEmpty());
            assertEquals("https://img.example/age.jpg", channel.getAvatars().get(0).getUrl());
            assertEquals(List.of("videos", "shorts", "livestreams"), channel.getTabs().stream()
                    .map(tab -> tab.getContentFilters().get(0)).toList());
        } finally {
            YoutubeParsingHelper.resetClientVersion();
            VyonEx.init(previous);
        }
    }

    private static YoutubeChannelExtractor channel(final String id) throws Exception {
        return new YoutubeChannelExtractor(new YoutubeService(1),
                YoutubeChannelLinkHandlerFactory.getInstance().fromUrl(
                        "https://www.youtube.com/channel/" + id));
    }

    private static JsonObject normalChannel(final JsonObject header) {
        final JsonArray tabs = new JsonArray();
        for (final String name : List.of("videos", "shorts", "streams", "releases", "playlists")) {
            tabs.add(json("tabRenderer", json("endpoint", json("commandMetadata",
                    json("webCommandMetadata", json("url", "/@creator/" + name))))));
        }
        return json("header", header,
                "metadata", json("channelMetadataRenderer", json("description", "Channel description")),
                "microformat", json("microformatDataRenderer",
                        json("tags", new JsonArray(List.of("music", "live")))),
                "contents", json("twoColumnBrowseResultsRenderer", json("tabs", tabs)),
                "padding", "x".repeat(60));
    }

    private static JsonObject json(final Object... fields) {
        final JsonObject object = new JsonObject();
        for (int index = 0; index < fields.length; index += 2) {
            object.put((String) fields[index], fields[index + 1]);
        }
        return object;
    }

    private static final class StubDownloader extends Downloader {
        private final String body;
        private final List<Request> requests = new ArrayList<>();

        private StubDownloader(final JsonObject response) { body = JsonWriter.string(response); }

        @Override
        public Response execute(final Request request) throws IOException, ReCaptchaException {
            requests.add(request);
            if (request.url().endsWith("/sw.js")) {
                return new Response(200, "OK", Map.of(),
                        "INNERTUBE_CONTEXT_CLIENT_VERSION\":\"2.20261022.01.00\"",
                        request.url());
            }
            return new Response(200, "OK", Map.of("Content-Type", List.of("application/json")),
                    body, request.url());
        }
    }
}
