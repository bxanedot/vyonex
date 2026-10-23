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
import java.util.ArrayList;
import java.util.List;

import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.ALR_YES;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.RN_0;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.SQ_0;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.SEGMENT_TIMELINE;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.buildAndCacheResult;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.generateDocumentAndDoCommonElementsGeneration;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.generateSegmentTemplateElement;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.generateSegmentTimelineElement;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.getInitializationResponse;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.setAttribute;

public final class YoutubeOtfDashManifestCreator {
    private static final String DURATION_MARKER = "Segment-Durations-Ms: ";
    private static final ManifestCreatorCache<String, String> CACHE =
            new ManifestCreatorCache<>();

    private YoutubeOtfDashManifestCreator() { }

    @Nonnull
    public static String fromOtfStreamingUrl(@Nonnull final String baseUrl,
                                             @Nonnull final ItagItem itag,
                                             final long durationSecondsFallback)
            throws CreationException {
        final var cached = CACHE.get(baseUrl);
        if (cached != null) return cached.getSecond();

        final Response response = getInitializationResponse(baseUrl, itag, DeliveryType.OTF);
        if (response.responseCode() != 200) {
            throw new CreationException("Could not get the initialization URL: response code "
                    + response.responseCode());
        }

        final List<Segment> segments = parseSegments(response.responseBody());
        final long durationMs = duration(segments, durationSecondsFallback);
        final String streamUrl = response.latestUrl().replace(SQ_0, "").replace(RN_0, "")
                .replace(ALR_YES, "");
        final Document document = generateDocumentAndDoCommonElementsGeneration(itag, durationMs);
        generateSegmentTemplateElement(document, streamUrl, DeliveryType.OTF);
        generateSegmentTimelineElement(document);
        appendSegments(document, segments);
        return buildAndCacheResult(baseUrl, document, CACHE);
    }

    @Nonnull
    public static ManifestCreatorCache<String, String> getCache() {
        return CACHE;
    }

    private static List<Segment> parseSegments(final String body) {
        final int marker = body.indexOf(DURATION_MARKER);
        if (marker < 0) throw new CreationException("Could not get segment durations");

        final String line = body.substring(marker + DURATION_MARKER.length()).split("\\R", 2)[0];
        final List<Segment> result = new ArrayList<>();
        for (final String value : line.split(",")) {
            final String part = value.strip();
            if (part.isEmpty()) continue;

            final int repeatMarker = part.indexOf("(r=");
            final String durationText = repeatMarker < 0 ? part : part.substring(0, repeatMarker);
            final int durationMs;
            final int repeat;
            try {
                durationMs = Integer.parseInt(durationText.strip());
                if (repeatMarker < 0) {
                    repeat = -1;
                } else {
                    String repeatText = part.substring(repeatMarker + 3).strip();
                    if (repeatText.endsWith(")")) {
                        repeatText = repeatText.substring(0, repeatText.length() - 1);
                    }
                    repeat = Integer.parseInt(repeatText);
                }
            } catch (final NumberFormatException e) {
                throw new CreationException("Could not get segment durations", e);
            }
            if (durationMs <= 0 || repeat < -1) {
                throw new CreationException("Invalid segment duration: " + part);
            }
            result.add(new Segment(durationMs, repeat));
        }
        if (result.isEmpty()) throw new CreationException("Could not get segment durations");
        return result;
    }

    private static long duration(final List<Segment> segments,
                                 final long durationSecondsFallback) {
        try {
            long total = 0;
            for (final Segment segment : segments) {
                final long count = segment.repeat < 0 ? 1 : (long) segment.repeat + 1;
                total = Math.addExact(total, Math.multiplyExact(segment.durationMs, count));
            }
            return total;
        } catch (final ArithmeticException e) {
            if (durationSecondsFallback > 0) return durationSecondsFallback * 1000;
            throw new CreationException("Could not determine stream duration", e);
        }
    }

    private static void appendSegments(final Document document, final List<Segment> segments) {
        try {
            final Element timeline = (Element) document.getElementsByTagName(
                    SEGMENT_TIMELINE).item(0);
            for (final Segment segment : segments) {
                final Element element = document.createElement("S");
                if (segment.repeat >= 0) {
                    setAttribute(element, document, "r", String.valueOf(segment.repeat));
                }
                setAttribute(element, document, "d", String.valueOf(segment.durationMs));
                timeline.appendChild(element);
            }
        } catch (final DOMException e) {
            throw CreationException.couldNotAddElement("segment (S)", e);
        }
    }

    private record Segment(long durationMs, int repeat) { }
}
