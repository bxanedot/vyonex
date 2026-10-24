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

class YoutubeShortsLockupInfoItemExtractorContractTest {
    @Test
    void mapsPrimaryRouteAndThumbnailLayout() throws Exception {
        final var extractor = new YoutubeShortsLockupInfoItemExtractor(JsonParser.object().from("""
                {
                  "overlayMetadata":{
                    "primaryText":{"content":"Short clip"},
                    "secondaryText":{"content":"1.2K views"}
                  },
                  "onTap":{"innertubeCommand":{"reelWatchEndpoint":{"videoId":"abcdefghijk"}}},
                  "thumbnail":{"sources":[{"url":"//img.example/short.jpg","width":360,"height":640}]}
                }
                """));

        assertEquals("Short clip", extractor.getName());
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", extractor.getUrl());
        assertEquals("https://img.example/short.jpg", extractor.getThumbnails().get(0).getUrl());
        assertEquals(StreamType.VIDEO_STREAM, extractor.getStreamType());
        assertEquals(1200, extractor.getViewCount());
        assertTrue(extractor.isShortFormContent());
        assertFalse(extractor.isAd());
        assertEquals(-1, extractor.getDuration());
        assertNull(extractor.getUploaderName());
        assertNull(extractor.getUploaderUrl());
        assertFalse(extractor.isUploaderVerified());
        assertNull(extractor.getTextualUploadDate());
        assertNull(extractor.getUploadDate());
    }

    @Test
    void usesInlinePlayerRouteAndNestedThumbnailLayout() throws Exception {
        final var extractor = new YoutubeShortsLockupInfoItemExtractor(JsonParser.object().from("""
                {
                  "overlayMetadata":{"primaryText":{"content":"Another short"},"secondaryText":{"content":"No views"}},
                  "inlinePlayerData":{"onVisible":{"innertubeCommand":{"watchEndpoint":{"videoId":"zyxwvutsrq1"}}}},
                  "thumbnailViewModel":{"thumbnailViewModel":{"image":{"sources":[
                    {"url":"https://img.example/nested.jpg","width":180,"height":320}
                  ]}}}
                }
                """));

        assertEquals("https://www.youtube.com/watch?v=zyxwvutsrq1", extractor.getUrl());
        assertEquals("https://img.example/nested.jpg", extractor.getThumbnails().get(0).getUrl());
        assertEquals(0, extractor.getViewCount());
    }

    @Test
    void handlesMembersOnlyAndRejectsMissingRequiredFields() throws Exception {
        final var membersOnly = new YoutubeShortsLockupInfoItemExtractor(JsonParser.object().from(
                "{\"overlayMetadata\":{\"secondaryText\":{\"content\":\"✪ Members only\"}}}"));
        final var missing = new YoutubeShortsLockupInfoItemExtractor(JsonParser.object().from("{}"));

        assertEquals(-1, membersOnly.getViewCount());
        assertThrows(ParsingException.class, missing::getName);
        assertThrows(ParsingException.class, missing::getUrl);
        assertThrows(ParsingException.class, missing::getViewCount);
    }
}
