// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeReelInfoItemExtractorContractTest {
    @Test
    void mapsReelMetadataAndShortViewCounts() throws Exception {
        final var reel = new YoutubeReelInfoItemExtractor(JsonParser.object().from("""
                {
                  "videoId":"abcdefghijk",
                  "headline":{"simpleText":"Short clip"},
                  "thumbnail":{"thumbnails":[{"url":"//img.example/short.jpg","width":360,"height":640}]},
                  "viewCountText":{"simpleText":"1.2K views"}
                }
                """));

        assertEquals("Short clip", reel.getName());
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", reel.getUrl());
        assertEquals("https://img.example/short.jpg", reel.getThumbnails().get(0).getUrl());
        assertEquals(StreamType.VIDEO_STREAM, reel.getStreamType());
        assertEquals(1200, reel.getViewCount());
        assertTrue(reel.isShortFormContent());
        assertFalse(reel.isAd());
        assertEquals(-1, reel.getDuration());
        assertNull(reel.getUploaderName());
        assertNull(reel.getUploaderUrl());
        assertFalse(reel.isUploaderVerified());
        assertNull(reel.getTextualUploadDate());
        assertNull(reel.getUploadDate());
    }

    @Test
    void treatsTheNoViewsLabelAsZeroAndRejectsMissingCount() throws Exception {
        final var noViews = new YoutubeReelInfoItemExtractor(JsonParser.object().from(
                "{\"viewCountText\":{\"simpleText\":\"No views\"}}"));
        final var missingCount = new YoutubeReelInfoItemExtractor(JsonParser.object().from("{}"));

        assertEquals(0, noViews.getViewCount());
        assertThrows(ParsingException.class, missingCount::getViewCount);
        assertThrows(ParsingException.class, missingCount::getUrl);
    }
}
