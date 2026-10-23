// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeShowRendererInfoItemExtractorContractTest {
    @Test
    void mapsShowTitleRouteThumbnailCountAndUploader() throws Exception {
        final var extractor = new YoutubeShowRendererInfoItemExtractor(JsonParser.object().from("""
                {
                  "title":"Collection",
                  "navigationEndpoint":{"browseEndpoint":{"browseId":"VLPL55"}},
                  "thumbnailRenderer":{"showCustomThumbnailRenderer":{"thumbnail":{"thumbnails":[
                    {"url":"//img.example/collection.jpg","width":480,"height":360}
                  ]}}},
                  "thumbnailOverlays":{"thumbnailOverlayBottomPanelRenderer":{"text":{"simpleText":"1.2K videos"}}},
                  "longBylineText":{"runs":[{"text":"Creator","navigationEndpoint":{"browseEndpoint":{"browseId":"UC123"}}}]},
                  "shortBylineText":{"runs":[{"text":"Fallback creator"}]}
                }
                """));

        assertEquals("Collection", extractor.getName());
        assertEquals("https://www.youtube.com/playlist?list=PL55", extractor.getUrl());
        assertEquals("https://img.example/collection.jpg", extractor.getThumbnails().get(0).getUrl());
        assertEquals(1200, extractor.getStreamCount());
        assertEquals("Creator", extractor.getUploaderName());
        assertEquals("https://www.youtube.com/channel/UC123", extractor.getUploaderUrl());
        assertFalse(extractor.isUploaderVerified());
    }

    @Test
    void reportsMissingRequiredShowData() throws Exception {
        final var extractor = new YoutubeShowRendererInfoItemExtractor(JsonParser.object().from("{}"));

        assertThrows(ParsingException.class, extractor::getName);
        assertThrows(ParsingException.class, extractor::getUrl);
        assertThrows(ParsingException.class, extractor::getStreamCount);
    }
}
