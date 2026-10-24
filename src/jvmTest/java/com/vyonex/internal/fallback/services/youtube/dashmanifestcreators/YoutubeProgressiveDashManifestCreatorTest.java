// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.dashmanifestcreators;

import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.services.youtube.ItagItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeProgressiveDashManifestCreatorTest {
    @BeforeEach
    void clearManifestCache() {
        YoutubeProgressiveDashManifestCreator.getCache().clear();
    }

    @Test
    void createsRangeBasedManifestFromItagDetails() throws Exception {
        final String xml = YoutubeProgressiveDashManifestCreator.fromProgressiveStreamingUrl(
                "https://media.example/video?key=a&part=b", itag(12_345), 60);
        final Document document = parse(xml);

        assertEquals("PT12.345S", document.getDocumentElement()
                .getAttribute("mediaPresentationDuration"));
        assertEquals("640", document.getElementsByTagName("Representation").item(0)
                .getAttributes().getNamedItem("width").getNodeValue());
        assertEquals("https://media.example/video?key=a&part=b",
                document.getElementsByTagName("BaseURL").item(0).getTextContent());
        assertEquals("10-20", document.getElementsByTagName("SegmentBase").item(0)
                .getAttributes().getNamedItem("indexRange").getNodeValue());
        assertEquals("0-9", document.getElementsByTagName("Initialization").item(0)
                .getAttributes().getNamedItem("range").getNodeValue());
    }

    @Test
    void usesSecondsFallbackWhenItagHasNoDuration() throws Exception {
        final String xml = YoutubeProgressiveDashManifestCreator.fromProgressiveStreamingUrl(
                "https://media.example/fallback", itag(ItagItem.APPROX_DURATION_MS_UNKNOWN), 7);

        assertEquals("PT7.000S", parse(xml).getDocumentElement()
                .getAttribute("mediaPresentationDuration"));
    }

    @Test
    void rejectsUnknownDurationAndInvalidByteRanges() {
        final ItagItem unknownDuration = itag(ItagItem.APPROX_DURATION_MS_UNKNOWN);
        assertThrows(CreationException.class,
                () -> YoutubeProgressiveDashManifestCreator.fromProgressiveStreamingUrl(
                        "https://media.example/no-duration", unknownDuration, 0));

        final ItagItem invalidRanges = itag(1_000);
        invalidRanges.setIndexStart(-1);
        assertThrows(CreationException.class,
                () -> YoutubeProgressiveDashManifestCreator.fromProgressiveStreamingUrl(
                        "https://media.example/no-range", invalidRanges, 1));
    }

    private static ItagItem itag(final long durationMs) {
        final ItagItem itag = new ItagItem(18, ItagItem.ItagType.VIDEO, MediaFormat.MPEG_4,
                "360p", 30);
        itag.setBitrate(1_000_000);
        itag.setWidth(640);
        itag.setHeight(360);
        itag.setCodec("avc1.64001E");
        itag.setApproxDurationMs(durationMs);
        itag.setIndexStart(10);
        itag.setIndexEnd(20);
        itag.setInitStart(0);
        itag.setInitEnd(9);
        return itag;
    }

    private static Document parse(final String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
