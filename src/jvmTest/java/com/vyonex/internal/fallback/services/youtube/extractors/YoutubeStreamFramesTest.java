// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeStreamFramesTest {
    @Test
    void parsesStoryboardLevelsAndPages() throws Exception {
        final var response = JsonParser.object().from("{\"storyboards\":{"
                + "\"playerStoryboardSpecRenderer\":{\"spec\":"
                + "\"https://img.example/$L/$N/M$M.jpg|160#90#5#2#2#1000#token#sig"
                + "|80#45#3#3#1#500#small#smallSig\"}}}");

        final var frames = YoutubeStreamExtractor.parseStoryboardFrames(response);

        assertEquals(2, frames.size());
        assertEquals("https://img.example/0/token/M0.jpg&sigh=sig", frames.get(0).getUrls().get(0));
        assertEquals("https://img.example/0/token/M1.jpg&sigh=sig", frames.get(0).getUrls().get(1));
        assertEquals(5, frames.get(0).getTotalCount());
        assertEquals(160, frames.get(0).getFrameWidth());
        assertEquals(3, frames.get(1).getTotalCount());
        assertEquals("https://img.example/1/small/M0.jpg&sigh=smallSig",
                frames.get(1).getUrls().get(0));
    }

    @Test
    void prefersLiveStoryboardAndReturnsEmptyWhenMissing() throws Exception {
        final var response = JsonParser.object().from("{\"storyboards\":{"
                + "\"playerLiveStoryboardSpecRenderer\":{\"spec\":"
                + "\"https://img.example/$L/$N.jpg|10#10#1#1#1#100#live#sig\"},"
                + "\"playerStoryboardSpecRenderer\":{\"spec\":\"\"}}}");

        final var frames = YoutubeStreamExtractor.parseStoryboardFrames(response);

        assertEquals(1, frames.size());
        assertEquals("https://img.example/0/live.jpg&sigh=sig", frames.get(0).getUrls().get(0));
        assertEquals(0, YoutubeStreamExtractor.parseStoryboardFrames(JsonParser.object().from("{}"))
                .size());
    }

    @Test
    void wrapsMalformedStoryboardNumbers() throws Exception {
        final var response = JsonParser.object().from("{\"storyboards\":{"
                + "\"playerStoryboardSpecRenderer\":{\"spec\":"
                + "\"https://img.example/$L/$N.jpg|bad#10#1#1#1#100#token#sig\"}}}");

        assertThrows(ExtractionException.class,
                () -> YoutubeStreamExtractor.parseStoryboardFrames(response));
    }
}
