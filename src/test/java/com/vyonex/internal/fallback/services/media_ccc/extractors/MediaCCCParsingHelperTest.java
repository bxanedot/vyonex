// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void extractsLiveStreamImageFields() throws JsonParserException {
        final JsonObject item = JsonParser.object().from("""
                {"thumb":"https://example.test/live.jpg","poster":"https://example.test/poster.jpg"}
                """);

        final List<Image> images = MediaCCCParsingHelper.getThumbnailsFromLiveStreamItem(item);

        assertEquals("https://example.test/live.jpg", images.get(0).getUrl());
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

    @Test
    void fetchesLiveStreamsFromEachCallDownloader() throws Exception {
        final JsonArray first = MediaCCCParsingHelper.getLiveStreams(
                liveStreamsDownloader("first"), Localization.DEFAULT);
        final JsonArray second = MediaCCCParsingHelper.getLiveStreams(
                liveStreamsDownloader("second"), Localization.DEFAULT);

        assertEquals("first", first.getObject(0).getString("slug"));
        assertEquals("second", second.getObject(0).getString("slug"));
    }

    @Test
    void rejectsFailedLiveStreamResponses() {
        final Downloader failed = new Downloader() {
            @Override
            public Response execute(final Request request) {
                return new Response(503, "Unavailable", Map.of(), "[]", request.url());
            }
        };
        final Downloader noResponse = new Downloader() {
            @Override public Response execute(final Request request) { return null; }
        };

        assertThrows(ExtractionException.class, () -> MediaCCCParsingHelper.getLiveStreams(
                failed, Localization.DEFAULT));
        assertThrows(ExtractionException.class, () -> MediaCCCParsingHelper.getLiveStreams(
                noResponse, Localization.DEFAULT));
    }

    private static Downloader liveStreamsDownloader(final String slug) {
        return new Downloader() {
            @Override
            public Response execute(final Request request) throws IOException {
                if (!"https://streaming.media.ccc.de/streams/v2.json".equals(request.url())) {
                    throw new IOException("Unexpected URL " + request.url());
                }
                return new Response(200, "OK", Map.of(), "[{\"slug\":\"" + slug + "\"}]",
                        request.url());
            }
        };
    }
}
