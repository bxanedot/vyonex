// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.subscription.SubscriptionItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoundcloudSubscriptionExtractorTest {
    @Test
    void normalizesChannelUrlFormsBeforeResolvingFollowingsOffline() throws Exception {
        final String homepage = "https://soundcloud.com";
        final String script = "https://cdn.sndcdn.com/assets/client.js";
        final String resolve = "https://api-widget.soundcloud.com/resolve?url="
                + "https%3A%2F%2Fsoundcloud.com%2Falice&format=json&client_id=offline-key";
        final String followings = "https://api-v2.soundcloud.com/users/7/followings"
                + "?client_id=offline-key&limit=200";
        final StubDownloader downloader = new StubDownloader(Map.of(
                homepage, "<script src=\"" + script + "\"></script>",
                script, "client_id:\"offline-key\"",
                resolve, "{\"id\":7}",
                followings, """
                        {"collection":[{"username":"Bob","permalink_url":"http://soundcloud.com/bob",
                          "followers_count":3,"track_count":2}],"next_href":""}
                        """));
        final Downloader previous = VyonEx.getDownloader();
        try {
            VyonEx.init(downloader);
            final var extractor = new SoundcloudService(31).getSubscriptionExtractor();
            for (final String input : List.of("alice", "/alice", "soundcloud.com/alice",
                    "www.soundcloud.com/alice", "http://soundcloud.com/alice")) {
                final List<SubscriptionItem> items = extractor.fromChannelUrl(input);
                assertEquals(1, items.size());
                assertEquals("Bob", items.get(0).getName());
                assertEquals("https://soundcloud.com/bob", items.get(0).getUrl());
            }
            assertEquals(5, downloader.requestedUrls.stream().filter(resolve::equals).count());
            assertEquals(5, downloader.requestedUrls.stream().filter(followings::equals).count());
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
