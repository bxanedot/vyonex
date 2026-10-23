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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubePostLiveStreamDvrDashManifestCreatorTest {
    private Downloader previousDownloader;
    private StubDownloader downloader;

    @BeforeEach
    void installDownloader() {
        previousDownloader = VyonEx.getDownloader();
        YoutubePostLiveStreamDvrDashManifestCreator.getCache().clear();
        downloader = new StubDownloader(new Response(200, "OK", Map.of(
                "X-Head-Time-Millis", List.of("123456"),
                "X-Head-Seqnum", List.of("5")), "", "https://media.example/live?itag=18"
                + "&sq=0&rn=0&alr=yes"));
        VyonEx.init(downloader);
    }

    @AfterEach
    void restoreDownloader() {
        VyonEx.init(previousDownloader);
    }

    @Test
    void buildsTimelineUsingInitializationResponse() throws Exception {
        final String xml = YoutubePostLiveStreamDvrDashManifestCreator
                .fromPostLiveStreamDvrStreamingUrl("https://media.example/live?itag=18",
                        itag(), 2, 60);
        final Document document = parse(xml);

        assertEquals("PT123.456S", document.getDocumentElement()
                .getAttribute("mediaPresentationDuration"));
        assertEquals("0", document.getElementsByTagName("SegmentTemplate").item(0)
                .getAttributes().getNamedItem("startNumber").getNodeValue());
        assertEquals("https://media.example/live?itag=18&sq=$Number$",
                document.getElementsByTagName("SegmentTemplate").item(0)
                        .getAttributes().getNamedItem("media").getNodeValue());
        assertEquals("2000", document.getElementsByTagName("S").item(0)
                .getAttributes().getNamedItem("d").getNodeValue());
        assertEquals("5", document.getElementsByTagName("S").item(0)
                .getAttributes().getNamedItem("r").getNodeValue());
        assertEquals("https://media.example/live?itag=18&sq=0&rn=0",
                downloader.request().url());
    }

    @Test
    void convertsFallbackSecondsToManifestMilliseconds() throws Exception {
        downloader = new StubDownloader(new Response(200, "OK", Map.of(
                "X-Head-Time-Millis", List.of("unknown"),
                "X-Head-Seqnum", List.of("3")), "", "https://media.example/live?itag=18"));
        VyonEx.init(downloader);

        final String xml = YoutubePostLiveStreamDvrDashManifestCreator
                .fromPostLiveStreamDvrStreamingUrl("https://media.example/live?itag=18",
                        itag(), 2, 15);

        assertEquals("PT15.000S", parse(xml).getDocumentElement()
                .getAttribute("mediaPresentationDuration"));
    }

    @Test
    void rejectsMissingHeadersAndNonPositiveTargetDuration() {
        downloader = new StubDownloader(new Response(200, "OK", Map.of(), "",
                "https://media.example/live?itag=18"));
        VyonEx.init(downloader);
        assertThrows(CreationException.class, () -> YoutubePostLiveStreamDvrDashManifestCreator
                .fromPostLiveStreamDvrStreamingUrl("https://media.example/live?itag=18",
                        itag(), 2, 15));

        assertThrows(CreationException.class, () -> YoutubePostLiveStreamDvrDashManifestCreator
                .fromPostLiveStreamDvrStreamingUrl("https://media.example/invalid-target",
                        itag(), 0, 15));
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

        private Request request() {
            return request;
        }
    }
}
