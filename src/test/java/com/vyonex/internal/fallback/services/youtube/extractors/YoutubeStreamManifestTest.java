// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import org.junit.jupiter.api.Test;

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
}
