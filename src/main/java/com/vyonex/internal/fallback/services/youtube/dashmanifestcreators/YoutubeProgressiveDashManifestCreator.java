// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.dashmanifestcreators;

import com.vyonex.internal.fallback.services.youtube.ItagItem;
import com.vyonex.internal.fallback.utils.ManifestCreatorCache;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;

import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.BASE_URL;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.INITIALIZATION;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.MPD;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.REPRESENTATION;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.SEGMENT_BASE;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.buildAndCacheResult;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.generateDocumentAndDoCommonElementsGeneration;
import static com.vyonex.internal.fallback.services.youtube.dashmanifestcreators.YoutubeDashManifestCreatorsUtils.setAttribute;

public final class YoutubeProgressiveDashManifestCreator {
    private static final ManifestCreatorCache<String, String> CACHE =
            new ManifestCreatorCache<>();

    private YoutubeProgressiveDashManifestCreator() { }

    @Nonnull
    public static String fromProgressiveStreamingUrl(@Nonnull final String baseUrl,
                                                    @Nonnull final ItagItem itag,
                                                    final long durationSecondsFallback)
            throws CreationException {
        final var cached = CACHE.get(baseUrl);
        if (cached != null) return cached.getSecond();

        long durationMs = itag.getApproxDurationMs();
        if (durationMs == ItagItem.APPROX_DURATION_MS_UNKNOWN) {
            if (durationSecondsFallback <= 0) {
                throw CreationException.couldNotAddElement(MPD,
                        "the duration of the stream could not be determined and "
                                + "durationSecondsFallback is <= 0");
            }
            durationMs = durationSecondsFallback * 1000;
        }

        final Document document = generateDocumentAndDoCommonElementsGeneration(itag, durationMs);
        final Element representation = (Element) document.getElementsByTagName(REPRESENTATION)
                .item(0);
        appendBaseUrl(document, representation, baseUrl);
        appendRanges(document, representation, itag);
        return buildAndCacheResult(baseUrl, document, CACHE);
    }

    @Nonnull
    public static ManifestCreatorCache<String, String> getCache() {
        return CACHE;
    }

    private static void appendBaseUrl(final Document document, final Element representation,
                                      final String baseUrl) {
        try {
            final Element base = document.createElement(BASE_URL);
            base.setTextContent(baseUrl);
            representation.appendChild(base);
        } catch (final DOMException e) {
            throw CreationException.couldNotAddElement(BASE_URL, e);
        }
    }

    private static void appendRanges(final Document document, final Element representation,
                                     final ItagItem itag) {
        final int indexStart = itag.getIndexStart();
        final int indexEnd = itag.getIndexEnd();
        final String indexRange = indexStart + "-" + indexEnd;

        if (indexStart < 0 || indexEnd < 0) {
            throw CreationException.couldNotAddElement(SEGMENT_BASE,
                    "ItagItem's indexStart or indexEnd are < 0: " + indexRange);
        }

        final Element segmentBase;
        try {
            segmentBase = document.createElement(SEGMENT_BASE);
            setAttribute(segmentBase, document, "indexRange", indexRange);
            representation.appendChild(segmentBase);
        } catch (final DOMException e) {
            throw CreationException.couldNotAddElement(SEGMENT_BASE, e);
        }

        final int initStart = itag.getInitStart();
        final int initEnd = itag.getInitEnd();
        final String initRange = initStart + "-" + initEnd;
        if (initStart < 0 || initEnd < 0) {
            throw CreationException.couldNotAddElement(INITIALIZATION,
                    "ItagItem's initStart and/or initEnd are/is < 0: " + initRange);
        }

        try {
            final Element initialization = document.createElement(INITIALIZATION);
            setAttribute(initialization, document, "range", initRange);
            segmentBase.appendChild(initialization);
        } catch (final DOMException e) {
            throw CreationException.couldNotAddElement(INITIALIZATION, e);
        }
    }
}
