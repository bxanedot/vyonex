// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubePlaylistInfoItemExtractorContractTest {
    @Test
    void mapsSearchPlaylistMetadataAndAbbreviatedCount() throws Exception {
        final var extractor = new YoutubePlaylistInfoItemExtractor(JsonParser.object().from("""
                {
                  "playlistId":"PLabcdefghijk",
                  "title":{"simpleText":"Ambient collection"},
                  "thumbnails":[{"thumbnails":[{"url":"//img.example/playlist.jpg","width":320,"height":180}]}],
                  "longBylineText":{"runs":[{"text":"Artist","navigationEndpoint":{"browseEndpoint":{"browseId":"UC123"}}}]},
                  "ownerBadges":[{"metadataBadgeRenderer":{"style":"BADGE_STYLE_TYPE_VERIFIED"}}],
                  "videoCountText":{"simpleText":"1.2K videos"}
                }
                """));

        assertEquals("Ambient collection", extractor.getName());
        assertEquals("https://www.youtube.com/playlist?list=PLabcdefghijk", extractor.getUrl());
        assertEquals("https://img.example/playlist.jpg", extractor.getThumbnails().get(0).getUrl());
        assertEquals("Artist", extractor.getUploaderName());
        assertEquals("https://www.youtube.com/channel/UC123", extractor.getUploaderUrl());
        assertTrue(extractor.isUploaderVerified());
        assertEquals(1200, extractor.getStreamCount());
    }

    @Test
    void usesAvailableThumbnailAndCountFieldsAndRejectsMissingCount() throws Exception {
        final var extractor = new YoutubePlaylistInfoItemExtractor(JsonParser.object().from("""
                {
                  "playlistId":"PLabcdefghijk",
                  "title":{"runs":[{"text":"Fallback playlist"}]},
                  "thumbnail":{"thumbnails":[{"url":"//img.example/fallback.jpg"}]},
                  "videoCount":"42"
                }
                """));
        final var missingCount = new YoutubePlaylistInfoItemExtractor(JsonParser.object().from("{}"));

        assertEquals("Fallback playlist", extractor.getName());
        assertEquals("https://img.example/fallback.jpg", extractor.getThumbnails().get(0).getUrl());
        assertEquals(42, extractor.getStreamCount());
        assertThrows(ParsingException.class, missingCount::getStreamCount);
    }
}
