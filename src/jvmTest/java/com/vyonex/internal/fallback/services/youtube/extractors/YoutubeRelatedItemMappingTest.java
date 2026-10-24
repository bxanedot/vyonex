// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class YoutubeRelatedItemMappingTest {
    @Test
    void mapsSupportedRendererTypes() throws Exception {
        assertInstanceOf(YoutubeStreamInfoItemExtractor.class, YoutubeStreamExtractor
                .parseRelatedItem(JsonParser.object().from("{\"compactVideoRenderer\":{}}"), null));
        assertInstanceOf(YoutubeMixOrPlaylistInfoItemExtractor.class, YoutubeStreamExtractor
                .parseRelatedItem(JsonParser.object().from("{\"compactRadioRenderer\":{}}"), null));
        assertInstanceOf(YoutubeMixOrPlaylistInfoItemExtractor.class, YoutubeStreamExtractor
                .parseRelatedItem(JsonParser.object().from("{\"compactPlaylistRenderer\":{}}"), null));
        assertInstanceOf(YoutubeMixOrPlaylistLockupInfoItemExtractor.class, YoutubeStreamExtractor
                .parseRelatedItem(JsonParser.object().from("{\"lockupViewModel\":{"
                        + "\"contentType\":\"LOCKUP_CONTENT_TYPE_PLAYLIST\"}}"), null));
        assertInstanceOf(YoutubeStreamInfoItemLockupExtractor.class, YoutubeStreamExtractor
                .parseRelatedItem(JsonParser.object().from("{\"lockupViewModel\":{"
                        + "\"contentType\":\"LOCKUP_CONTENT_TYPE_VIDEO\"}}"), null));
    }

    @Test
    void ignoresUnsupportedRendererTypes() throws Exception {
        assertNull(YoutubeStreamExtractor.parseRelatedItem(JsonParser.object().from("{}"), null));
        assertNull(YoutubeStreamExtractor.parseRelatedItem(JsonParser.object().from(
                "{\"lockupViewModel\":{\"contentType\":\"UNKNOWN\"}}"), null));
    }
}
