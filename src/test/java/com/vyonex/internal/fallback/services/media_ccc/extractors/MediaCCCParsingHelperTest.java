// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaCCCParsingHelperTest {
    @Test
    void recognizesCompleteLiveStreamIdsIncludingHyphenatedSlugs() {
        assertTrue(MediaCCCParsingHelper.isLiveStreamId("matrix-conf-2026/sofya"));
        assertFalse(MediaCCCParsingHelper.isLiveStreamId(
                "https://streaming.media.ccc.de/matrix-conf-2026/sofya"));
        assertFalse(MediaCCCParsingHelper.isLiveStreamId("matrix-conf-2026/sofya/extra"));
    }

    @Test
    void extractsAvailableThumbnailsInQualityOrder() throws JsonParserException {
        final JsonObject item = JsonParser.object().from(
                "{\"thumb_url\":\"https://example.test/thumb.jpg\","
                        + "\"poster_url\":\"https://example.test/poster.jpg\"}"
        );

        final List<Image> images = MediaCCCParsingHelper.getThumbnailsFromStreamItem(item);

        assertEquals(2, images.size());
        assertEquals("https://example.test/thumb.jpg", images.get(0).getUrl());
        assertEquals(Image.ResolutionLevel.MEDIUM, images.get(0).getEstimatedResolutionLevel());
        assertEquals("https://example.test/poster.jpg", images.get(1).getUrl());
        assertEquals(Image.ResolutionLevel.HIGH, images.get(1).getEstimatedResolutionLevel());
    }

    @Test
    void ignoresMissingThumbnailAndLogoUrls() throws JsonParserException {
        final JsonObject item = JsonParser.object().from("{\"thumb_url\":\"\",\"poster_url\":null}");

        assertTrue(MediaCCCParsingHelper.getThumbnailsFromStreamItem(item).isEmpty());
        assertTrue(MediaCCCParsingHelper.getImageListFromLogoImageUrl(null).isEmpty());
        assertTrue(MediaCCCParsingHelper.getImageListFromLogoImageUrl("").isEmpty());
    }
}
