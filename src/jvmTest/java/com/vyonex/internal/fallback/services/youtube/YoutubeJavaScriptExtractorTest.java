// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeJavaScriptExtractorTest {
    private static final String PLAYER_PATH =
            "/s/player/abc12345/player_ias.vflset/en_US/base.js";

    @Test
    void readsPlayerUrlFromEmbeddedConfig() throws Exception {
        assertEquals(PLAYER_PATH, YoutubeJavaScriptExtractor.extractPlayerUrlFromEmbeddedPage(
                "{\"jsUrl\":\"" + PLAYER_PATH + "\"}"));
    }

    @Test
    void readsEscapedSlashesFromEmbeddedConfig() throws Exception {
        assertEquals(PLAYER_PATH,
                YoutubeJavaScriptExtractor.extractPlayerUrlFromEmbeddedPage(
                        "{\"jsUrl\":\"\\/s\\/player\\/abc12345"
                                + "\\/player_ias.vflset\\/en_US\\/base.js\"}"));
    }

    @Test
    void resolvesOnlySecureYoutubePlayerUrls() throws Exception {
        assertEquals("https://www.youtube.com" + PLAYER_PATH,
                YoutubeJavaScriptExtractor.cleanJavaScriptUrl(PLAYER_PATH));
        assertEquals("https://www.youtube.com" + PLAYER_PATH,
                YoutubeJavaScriptExtractor.cleanJavaScriptUrl("//www.youtube.com" + PLAYER_PATH));
        assertThrows(ParsingException.class,
                () -> YoutubeJavaScriptExtractor.cleanJavaScriptUrl(
                        "https://example.invalid/base.js"));
    }
}
