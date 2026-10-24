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
    void extractsAvailableThumbnailsInQualityOrder() {
        final List<Image> images = MediaCCCParsingHelper.getThumbnailsFromStreamItem(
                "https://example.test/thumb.jpg", "https://example.test/poster.jpg");

        assertEquals(2, images.size());
        assertEquals("https://example.test/thumb.jpg", images.get(0).getUrl());
        assertEquals(Image.ResolutionLevel.MEDIUM, images.get(0).getEstimatedResolutionLevel());
        assertEquals("https://example.test/poster.jpg", images.get(1).getUrl());
        assertEquals(Image.ResolutionLevel.HIGH, images.get(1).getEstimatedResolutionLevel());
        assertThrows(UnsupportedOperationException.class, () -> images.add(
                new Image("https://example.test/extra.jpg", Image.HEIGHT_UNKNOWN,
                        Image.WIDTH_UNKNOWN, Image.ResolutionLevel.UNKNOWN)));
    }

    @Test
    void extractsLiveStreamImageFields() {
        final List<Image> images = MediaCCCParsingHelper.getThumbnailsFromStreamItem(
                "https://example.test/live.jpg", "https://example.test/poster.jpg");

        assertEquals("https://example.test/live.jpg", images.get(0).getUrl());
        assertEquals(Image.ResolutionLevel.MEDIUM, images.get(0).getEstimatedResolutionLevel());
        assertEquals("https://example.test/poster.jpg", images.get(1).getUrl());
        assertEquals(Image.ResolutionLevel.HIGH, images.get(1).getEstimatedResolutionLevel());
    }

    @Test
    void ignoresMissingThumbnailAndLogoUrls() {
        assertTrue(MediaCCCParsingHelper.getThumbnailsFromStreamItem("", null).isEmpty());
        assertTrue(MediaCCCParsingHelper.getImageListFromLogoImageUrl(null).isEmpty());
        assertTrue(MediaCCCParsingHelper.getImageListFromLogoImageUrl("").isEmpty());
    }

    @Test
    void fetchesLiveStreamBodiesFromEachCallDownloader() throws Exception {
        final String first = MediaCCCParsingHelper.fetchLiveStreamsBody(
                liveStreamsDownloader("first"), Localization.DEFAULT);
        final String second = MediaCCCParsingHelper.fetchLiveStreamsBody(
                liveStreamsDownloader("second"), Localization.DEFAULT);

        assertTrue(first.contains("\"slug\":\"first\""));
        assertTrue(second.contains("\"slug\":\"second\""));
    }

    @Test
    void fetchesOnlySuccessfulApiBodies() throws Exception {
        final String url = "https://api.media.ccc.de/public/events/example";
        final Downloader successful = new Downloader() {
            @Override
            public Response execute(final Request request) {
                return new Response(200, "OK", Map.of(), "{\"title\":\"Talk\"}", request.url());
            }
        };
        final Downloader noResponse = new Downloader() {
            @Override public Response execute(final Request request) { return null; }
        };

        assertEquals("{\"title\":\"Talk\"}", MediaCCCParsingHelper.fetchJsonBody(
                successful, url, Localization.DEFAULT));
        assertThrows(ExtractionException.class, () -> MediaCCCParsingHelper.fetchJsonBody(
                noResponse, url, Localization.DEFAULT));
    }

    @Test
    void fetchesMalformedBodiesForTypedDecodersToReject() throws Exception {
        final String url = "https://api.media.ccc.de/public/events/example";
        final Downloader failed = new Downloader() {
            @Override
            public Response execute(final Request request) {
                return new Response(503, "Unavailable", Map.of(), "{}", request.url());
            }
        };
        final Downloader malformed = new Downloader() {
            @Override
            public Response execute(final Request request) {
                return new Response(200, "OK", Map.of(), "not-json", request.url());
            }
        };

        assertThrows(ExtractionException.class, () -> MediaCCCParsingHelper.fetchJsonBody(
                failed, url, Localization.DEFAULT));
        assertEquals("not-json", MediaCCCParsingHelper.fetchJsonBody(
                malformed, url, Localization.DEFAULT));
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

        assertThrows(ExtractionException.class, () -> MediaCCCParsingHelper.fetchLiveStreamsBody(
                failed, Localization.DEFAULT));
        assertThrows(ExtractionException.class, () -> MediaCCCParsingHelper.fetchLiveStreamsBody(
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
