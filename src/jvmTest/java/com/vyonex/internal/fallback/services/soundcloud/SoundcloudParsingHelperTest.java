// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.channel.ChannelInfoItemsCollector;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoundcloudParsingHelperTest {
    @Test
    void resolvesApiItemsAndAddsClientIdToPaginationOffline() throws Exception {
        final String homepage = "https://soundcloud.com";
        final String script = "https://a-v2.sndcdn.com/assets/app.js";
        final String resolve = SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
                + "resolve?url=https%3A%2F%2Fsoundcloud.com%2Fartist%2Ftrack"
                + "&client_id=offline-key";
        final String tracks = SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
                + "users/1/tracks?client_id=offline-key";
        final StubDownloader downloader = new StubDownloader(Map.of(
                homepage, "<script src=\"" + script + "\"></script>",
                script, "window.config={client_id:\"offline-key\"};",
                resolve, "{\"id\":\"42\",\"kind\":\"track\"}",
                tracks, "{\"collection\":[],\"next_href\":\""
                        + SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
                        + "users/1/tracks?offset=10\"}"));
        final Downloader previous = VyonEx.getDownloader();
        try {
            VyonEx.init(downloader);

            assertEquals("{\"id\":\"42\",\"kind\":\"track\"}",
                    SoundcloudParsingHelper.resolveFor(downloader,
                            "https://soundcloud.com/artist/track"));
            assertEquals(SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
                    + "users/1/tracks?offset=10&client_id=offline-key",
                    SoundcloudParsingHelper.getStreamsFromApi(
                            new StreamInfoItemsCollector(1), tracks));
            assertEquals(List.of(homepage, script, resolve, tracks), downloader.requestedUrls);
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void parsesSoundcloudDates() throws Exception {
        final var date = SoundcloudParsingHelper.parseDate("2026-10-22T12:30:00Z");
        assertEquals(Instant.parse("2026-10-22T12:30:00Z"), date.getInstant());
    }

    @Test
    void mapsSoundcloudArtworkAndVisualSizeVariants() {
        final List<Image> artwork = SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(
                "https://i1.sndcdn.com/artworks-abc-large.jpg");
        assertEquals(15, artwork.size());
        assertEquals("https://i1.sndcdn.com/artworks-abc-mini.jpg", artwork.get(0).getUrl());
        assertEquals(16, artwork.get(0).getWidth());
        assertEquals(Image.ResolutionLevel.LOW, artwork.get(0).getEstimatedResolutionLevel());
        assertEquals("https://i1.sndcdn.com/artworks-abc-t500x500.jpg",
                artwork.get(14).getUrl());
        assertEquals(500, artwork.get(14).getHeight());
        assertEquals(Image.ResolutionLevel.MEDIUM,
                artwork.get(14).getEstimatedResolutionLevel());
        final List<Image> queryArtwork = SoundcloudParsingHelper
                .getAllImagesFromArtworkOrAvatarUrl(
                        "https://i1.sndcdn.com/artworks-abc-large.jpg?token=a%20b");
        assertEquals("https://i1.sndcdn.com/artworks-abc-mini.jpg?token=a%20b",
                queryArtwork.get(0).getUrl());

        final List<Image> visuals = SoundcloudParsingHelper.getAllImagesFromVisualUrl(
                "https://i1.sndcdn.com/visuals-abc-original.jpg");
        assertEquals(List.of("https://i1.sndcdn.com/visuals-abc-t1240x260.jpg",
                        "https://i1.sndcdn.com/visuals-abc-t2480x520.jpg"),
                visuals.stream().map(Image::getUrl).toList());
        assertEquals(1240, visuals.get(0).getWidth());
        assertEquals(260, visuals.get(0).getHeight());
    }

    @Test
    void stopsWhenTheApiRepeatsTheSamePaginationUrl() throws Exception {
        final String url = "https://api-v2.soundcloud.com/users/7/followings?client_id=offline-key";
        final StubDownloader downloader = new StubDownloader(Map.of(url,
                "{\"collection\":[],\"next_href\":\"" + url + "\"}"));
        final Downloader previous = VyonEx.getDownloader();
        try {
            VyonEx.init(downloader);
            assertEquals("", SoundcloudParsingHelper.getUsersFromApiMinItems(1,
                    new ChannelInfoItemsCollector(1), url));
            assertEquals(List.of(url), downloader.requestedUrls);
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void mapsUserPagesFromOfflineApiResponses() throws Exception {
        final String url = "https://api-v2.soundcloud.com/users/7/followings?client_id=offline-key";
        final String next = "https://api-v2.soundcloud.com/users/7/followings?offset=50"
                + "&client_id=offline-key";
        final StubDownloader downloader = new StubDownloader(Map.of(url, """
                {"collection":[{"username":"Alice","permalink_url":"http://soundcloud.com/alice",
                  "avatar_url":"https://images.example/alice.jpg","followers_count":12,
                  "track_count":4,"description":"Producer","verified":true},null],
                 "next_href":"https://api-v2.soundcloud.com/users/7/followings?offset=50&client_id=offline-key"}
                """));
        final Downloader previous = VyonEx.getDownloader();
        try {
            VyonEx.init(downloader);
            final ChannelInfoItemsCollector users = new ChannelInfoItemsCollector(31);

            assertEquals(next, SoundcloudParsingHelper.getUsersFromApi(users, url));
            assertEquals(1, users.getItems().size());
            assertEquals("Alice", users.getItems().get(0).getName());
            assertEquals("https://soundcloud.com/alice", users.getItems().get(0).getUrl());
            assertEquals(12, users.getItems().get(0).getSubscriberCount());
            assertEquals(4, users.getItems().get(0).getStreamCount());
            assertEquals("Producer", users.getItems().get(0).getDescription());
            assertEquals(List.of(url), downloader.requestedUrls);
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void mapsTrackAndChartPagesFromOfflineApiResponses() throws Exception {
        final String tracksUrl = "https://api-v2.soundcloud.com/users/7/tracks?client_id=offline-key";
        final String chartsUrl = "https://api-v2.soundcloud.com/charts?client_id=offline-key";
        final StubDownloader downloader = new StubDownloader(Map.of(
                tracksUrl, """
                        {"collection":[{"id":42,"title":"Rain","duration":90000,
                          "permalink_url":"https://soundcloud.com/a/rain",
                          "user":{"username":"Alice"}}],"next_href":null}
                        """,
                chartsUrl, """
                        {"collection":[{"track":{"id":7,"title":"Wind",
                          "permalink_url":"https://soundcloud.com/a/wind"}}]}
                        """));
        final Downloader previous = VyonEx.getDownloader();
        try {
            VyonEx.init(downloader);
            final StreamInfoItemsCollector tracks = new StreamInfoItemsCollector(31);
            final StreamInfoItemsCollector charts = new StreamInfoItemsCollector(31);

            assertEquals("", SoundcloudParsingHelper.getStreamsFromApi(tracks, tracksUrl));
            assertEquals("", SoundcloudParsingHelper.getStreamsFromApi(charts, chartsUrl, true));
            assertEquals(List.of("Rain"), tracks.getItems().stream()
                    .map(item -> item.getName()).toList());
            assertEquals(List.of("Wind"), charts.getItems().stream()
                    .map(item -> item.getName()).toList());
            assertEquals(List.of(tracksUrl, chartsUrl), downloader.requestedUrls);
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void mapsMixedChannelTabPagesFromOfflineApiResponses() throws Exception {
        final String url = "https://api-v2.soundcloud.com/users/7/likes?client_id=offline-key";
        final StubDownloader downloader = new StubDownloader(Map.of(url, """
                {"collection":[{"kind":"user","username":"Alice"},
                  {"kind":"track","title":"Rain"},
                  {"kind":"playlist","title":"Mix"},
                  {"kind":"like","track":{"title":"Liked"}},
                  {"kind":"like","playlist":{"title":"Liked Mix"}}],
                 "next_href":"https://api-v2.soundcloud.com/users/7/likes?offset=20&client_id=offline-key"}
                """));
        final Downloader previous = VyonEx.getDownloader();
        try {
            VyonEx.init(downloader);
            final MultiInfoItemsCollector items = new MultiInfoItemsCollector(31);

            assertEquals("https://api-v2.soundcloud.com/users/7/likes?offset=20&client_id=offline-key",
                    SoundcloudParsingHelper.getInfoItemsFromApi(items, url));
            assertEquals(List.of("Alice", "Rain", "Mix", "Liked", "Liked Mix"),
                    items.getItems().stream().map(item -> item.getName()).toList());
            assertEquals(List.of(url), downloader.requestedUrls);
        } finally {
            VyonEx.init(previous);
        }
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
