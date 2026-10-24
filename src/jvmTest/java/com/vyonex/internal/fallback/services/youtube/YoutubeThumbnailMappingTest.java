// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeThumbnailMappingTest {
    @Test
    void normalizesProtocolRelativeAndInsecureThumbnailUrls() {
        assertEquals("https://img.example/a", YoutubeParsingHelper.fixThumbnailUrl(
                "http://img.example/a"));
        assertEquals("https://img.example/b", YoutubeParsingHelper.fixThumbnailUrl(
                "//img.example/b"));
        assertEquals("https://img.example/c", YoutubeParsingHelper.fixThumbnailUrl(
                "https://img.example/c"));
        assertEquals("https://img.example/d", YoutubeParsingHelper.fixThumbnailUrl(
                "img.example/d"));
    }

    @Test
    void mapsAvailableImageFieldsAndReturnsAnImmutableList() throws Exception {
        final List<Image> images = YoutubeParsingHelper.getImagesFromThumbnailsArray(
                JsonParser.array().from("[{\"url\":\"http://img.example/a\","
                        + "\"width\":1280,\"height\":720},{\"url\":\"//img.example/b\","
                        + "\"height\":120},{\"height\":10}]"));

        assertEquals(2, images.size());
        assertEquals("https://img.example/a", images.get(0).getUrl());
        assertEquals(1280, images.get(0).getWidth());
        assertEquals(Image.ResolutionLevel.HIGH, images.get(0).getEstimatedResolutionLevel());
        assertEquals("https://img.example/b", images.get(1).getUrl());
        assertEquals(Image.ResolutionLevel.LOW, images.get(1).getEstimatedResolutionLevel());
        assertThrows(UnsupportedOperationException.class,
                () -> images.add(images.get(0)));
    }
}
