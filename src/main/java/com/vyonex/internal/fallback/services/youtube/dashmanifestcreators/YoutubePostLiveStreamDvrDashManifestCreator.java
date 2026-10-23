// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.dashmanifestcreators;

import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.services.youtube.DeliveryType;
import com.vyonex.internal.fallback.services.youtube.ItagItem;
import com.vyonex.internal.fallback.utils.ManifestCreatorCache;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;

import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.ALR_YES;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.RN_0;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.SEGMENT_TIMELINE;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.SQ_0;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.buildAndCacheResult;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.generateDocumentAndDoCommonElementsGeneration;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.generateSegmentTemplateElement;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.generateSegmentTimelineElement;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.getInitializationResponse;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.setAttribute;

public final class YoutubePostLiveStreamDvrDashManifestCreator {
    private static final String DURATION_HEADER = "X-Head-Time-Millis";
    private static final String SEGMENT_COUNT_HEADER = "X-Head-Seqnum";
    private static final ManifestCreatorCache<String, String> CACHE =
            new ManifestCreatorCache<>();

    private YoutubePostLiveStreamDvrDashManifestCreator() { }

    @Nonnull
    public static String fromPostLiveStreamDvrStreamingUrl(
            @Nonnull final String baseUrl,
            @Nonnull final ItagItem itag,
            final int targetDurationSec,
            final long durationSecondsFallback) throws CreationException {
        final var cached = CACHE.get(baseUrl);
        if (cached != null) return cached.getSecond();
        if (targetDurationSec <= 0) {
            throw new CreationException("targetDurationSec value is <= 0: " + targetDurationSec);
        }

        final Response response = getInitializationResponse(baseUrl, itag, DeliveryType.LIVE);
        if (response.responseCode() != 200) {
            throw new CreationException("Could not get the initialization sequence: response code "
                    + response.responseCode());
        }

        final String durationHeader = response.getHeader(DURATION_HEADER);
        final String segmentCount = response.getHeader(SEGMENT_COUNT_HEADER);
        if (durationHeader == null || segmentCount == null) {
            throw new CreationException("Could not get the value of the " + DURATION_HEADER
                    + " or the " + SEGMENT_COUNT_HEADER + " header");
        }
        if (segmentCount.isBlank()) {
            throw new CreationException("Could not get the number of segments");
        }

        long durationMs;
        try {
            durationMs = Long.parseLong(durationHeader);
        } catch (final NumberFormatException e) {
            if (durationSecondsFallback <= 0) {
                throw new CreationException("Could not parse stream duration or use fallback", e);
            }
            durationMs = durationSecondsFallback * 1000;
        }

        return createManifest(baseUrl, cleanedUrl(response), itag, targetDurationSec, durationMs,
                segmentCount);
    }

    @Nonnull
    public static ManifestCreatorCache<String, String> getCache() {
        return CACHE;
    }

    private static String cleanedUrl(final Response response) {
        return response.latestUrl().replace(SQ_0, "").replace(RN_0, "").replace(ALR_YES, "");
    }

    private static String createManifest(final String originalUrl, final String streamUrl,
                                         final ItagItem itag, final int targetDurationSec,
                                         final long durationMs, final String segmentCount) {
        final Document document = generateDocumentAndDoCommonElementsGeneration(itag, durationMs);
        generateSegmentTemplateElement(document, streamUrl, DeliveryType.LIVE);
        generateSegmentTimelineElement(document);
        appendSegment(document, targetDurationSec, segmentCount);
        return buildAndCacheResult(originalUrl, document, CACHE);
    }

    private static void appendSegment(final Document document, final int targetDurationSec,
                                      final String segmentCount) {
        try {
            final Element timeline = (Element) document.getElementsByTagName(
                    SEGMENT_TIMELINE).item(0);
            final Element segment = document.createElement("S");
            setAttribute(segment, document, "d", String.valueOf((long) targetDurationSec * 1000));
            setAttribute(segment, document, "r", segmentCount);
            timeline.appendChild(segment);
        } catch (final DOMException e) {
            throw CreationException.couldNotAddElement("segment (S)", e);
        }
    }
}
