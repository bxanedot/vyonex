// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubePlayerResponseValidationTest {
    @Test
    void acceptsOnlyTheRequestedVideoResponse() throws Exception {
        assertTrue(YoutubeStreamExtractor.isPlayerResponseValid(JsonParser.object().from(
                "{\"videoDetails\":{\"videoId\":\"expected\"}}"), "expected"));
        assertFalse(YoutubeStreamExtractor.isPlayerResponseValid(JsonParser.object().from(
                "{\"videoDetails\":{\"videoId\":\"other\"}}"), "expected"));
        assertFalse(YoutubeStreamExtractor.isPlayerResponseValid(JsonParser.object().from("{}"),
                "expected"));
    }
}
