// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.dashmanifestcreators;

import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.services.youtube.ItagItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeOtfDashManifestCreatorTest {
    private Downloader previousDownloader;
    private StubDownloader downloader;

    @BeforeEach
    void installDownloader() {
        previousDownloader = VyonEx.getDownloader();
        YoutubeOtfDashManifestCreator.getCache().clear();
        downloader = new StubDownloader(response("Segment-Durations-Ms: 4000,2000(r=2),\n",
                "https://media.example/otf?itag=18&sq=0&rn=0"));
        VyonEx.init(downloader);
    }

    @AfterEach
    void restoreDownloader() {
        VyonEx.init(previousDownloader);
    }

    @Test
    void buildsTimelineFromFirstSequenceDurations() throws Exception {
        final String xml = YoutubeOtfDashManifestCreator.fromOtfStreamingUrl(
                "https://media.example/otf?itag=18", itag(), 60);
        final Document document = parse(xml);

        assertEquals("PT10.000S", document.getDocumentElement()
                .getAttribute("mediaPresentationDuration"));
        assertEquals("1", document.getElementsByTagName("SegmentTemplate").item(0)
                .getAttributes().getNamedItem("startNumber").getNodeValue());
        assertEquals("https://media.example/otf?itag=18&sq=$Number$",
                document.getElementsByTagName("SegmentTemplate").item(0)
                        .getAttributes().getNamedItem("media").getNodeValue());
        assertEquals("4000", document.getElementsByTagName("S").item(0)
                .getAttributes().getNamedItem("d").getNodeValue());
        assertEquals("2", document.getElementsByTagName("S").item(1)
                .getAttributes().getNamedItem("r").getNodeValue());
        assertEquals("2000", document.getElementsByTagName("S").item(1)
                .getAttributes().getNamedItem("d").getNodeValue());
        assertEquals("https://media.example/otf?itag=18&sq=0&rn=0",
                downloader.request.url());
    }

    @Test
    void rejectsMissingOrMalformedSegmentDurations() {
        downloader = new StubDownloader(response("no duration field", "https://media.example/otf"));
        VyonEx.init(downloader);
        assertThrows(CreationException.class, () -> YoutubeOtfDashManifestCreator
                .fromOtfStreamingUrl("https://media.example/no-header", itag(), 30));

        downloader = new StubDownloader(response("Segment-Durations-Ms: bad,\n",
                "https://media.example/otf"));
        VyonEx.init(downloader);
        assertThrows(CreationException.class, () -> YoutubeOtfDashManifestCreator
                .fromOtfStreamingUrl("https://media.example/bad-header", itag(), 30));
    }

    private static Response response(final String body, final String url) {
        return new Response(200, "OK", Map.of(), body, url);
    }

    private static ItagItem itag() {
        final ItagItem itag = new ItagItem(18, ItagItem.ItagType.VIDEO, MediaFormat.MPEG_4,
                "360p", 30);
        itag.setBitrate(1_000_000);
        itag.setWidth(640);
        itag.setHeight(360);
        itag.setCodec("avc1.64001E");
        return itag;
    }

    private static Document parse(final String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static final class StubDownloader extends Downloader {
        private final Response response;
        private Request request;

        private StubDownloader(final Response response) {
            this.response = response;
        }

        @Override
        public Response execute(final Request value) throws IOException, ReCaptchaException {
            request = value;
            return response;
        }
    }
}
