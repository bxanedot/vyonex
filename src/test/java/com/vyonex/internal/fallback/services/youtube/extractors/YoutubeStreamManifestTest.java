// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YoutubeStreamManifestTest {
    @Test
    void leavesManifestUrlUntouchedWhenItHasNoExtraParameters() {
        assertEquals("https://manifest.example/hls",
                YoutubeStreamExtractor.appendManifestParameters(
                        "https://manifest.example/hls", null, ""));
    }

    @Test
    void appendsTokenAndVersionToExistingManifestQuery() {
        assertEquals("https://manifest.example/dash?signature=abc&pot=token%2Bvalue&mpd_version=7",
                YoutubeStreamExtractor.appendManifestParameters(
                        "https://manifest.example/dash?signature=abc", "token+value",
                        "mpd_version=7"));
    }

    @Test
    void selectsTheFirstAvailableManifestAndUsesItsToken() throws Exception {
        final var empty = JsonParser.object().from("{}");
        final var apple = JsonParser.object().from("{\"hlsManifestUrl\":"
                + "\"https://manifest.example/apple?sig=one\"}");
        final var android = JsonParser.object().from("{\"hlsManifestUrl\":"
                + "\"https://manifest.example/android\"}");

        assertEquals("https://manifest.example/apple?sig=one&pot=apple-token",
                YoutubeStreamExtractor.getManifestUrl("hls", List.of(
                        new YoutubeStreamExtractor.ManifestSource(null, null),
                        new YoutubeStreamExtractor.ManifestSource(empty, null),
                        new YoutubeStreamExtractor.ManifestSource(apple, "apple-token"),
                        new YoutubeStreamExtractor.ManifestSource(android, "android-token")), ""));
    }
}
