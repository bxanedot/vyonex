// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeStreamMetadataTest {
    @Test
    void prefersOriginalTitleAndFallsBackToRenderedTitle() throws Exception {
        assertEquals("Original", YoutubeStreamExtractor.extractVideoTitle(
                JsonParser.object().from("{\"title\":\"Original\"}"),
                JsonParser.object().from("{\"title\":{\"simpleText\":\"Rendered\"}}")));
        assertEquals("Rendered", YoutubeStreamExtractor.extractVideoTitle(
                JsonParser.object().from("{}"),
                JsonParser.object().from("{\"title\":{\"simpleText\":\"Rendered\"}}")));
    }

    @Test
    void rejectsResponsesWithoutATitle() throws Exception {
        assertThrows(ParsingException.class, () -> YoutubeStreamExtractor.extractVideoTitle(
                JsonParser.object().from("{}"), JsonParser.object().from("{}")));
    }
}
