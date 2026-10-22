// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.channel.ChannelInfoItemsCollector;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.json.JsonParser;
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

            assertEquals("42", SoundcloudParsingHelper.resolveFor(downloader,
                    "https://soundcloud.com/artist/track").getString("id"));
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
    void parsesDateAndKeepsArtworkUrlWithoutGuessingDimensions() throws Exception {
        final var date = SoundcloudParsingHelper.parseDate("2026-10-22T12:30:00Z");
        assertEquals(Instant.parse("2026-10-22T12:30:00Z"), date.getInstant());

        final var track = JsonParser.object().from("""
                {"artwork_url":"http://images.example/cover.jpg"}
                """);
        final Image image = SoundcloudParsingHelper.getAllImagesFromTrackObject(track).get(0);
        assertEquals("https://images.example/cover.jpg", image.getUrl());
        assertEquals(Image.WIDTH_UNKNOWN, image.getWidth());
        assertEquals(Image.HEIGHT_UNKNOWN, image.getHeight());
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
