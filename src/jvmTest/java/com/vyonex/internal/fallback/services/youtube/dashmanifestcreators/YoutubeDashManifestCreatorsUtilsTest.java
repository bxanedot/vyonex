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
import com.vyonex.internal.fallback.services.youtube.DeliveryType;
import com.vyonex.internal.fallback.services.youtube.ItagItem;
import com.vyonex.internal.fallback.stream.AudioTrackType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeDashManifestCreatorsUtilsTest {
    private Downloader previousDownloader;
    private StubDownloader downloader;

    @BeforeEach
    void captureDownloader() {
        previousDownloader = VyonEx.getDownloader();
    }

    @AfterEach
    void restoreDownloader() {
        VyonEx.init(previousDownloader);
    }

    @Test
    void buildsAudioRepresentationWithLanguageAndTrackRole() throws Exception {
        final ItagItem audio = new ItagItem(140, ItagItem.ItagType.AUDIO, MediaFormat.WEBMA, 128);
        audio.setCodec("mp4a.40.2");
        audio.setBitrate(128_000);
        audio.setSampleRate(48_000);
        audio.setAudioChannels(2);
        audio.setAudioLocale(Locale.FRENCH);
        audio.setAudioTrackType(AudioTrackType.DUBBED);

        final Document document = YoutubeDashManifestCreatorsUtils
                .generateDocumentAndDoCommonElementsGeneration(audio, 3_000);

        assertEquals("fr", document.getElementsByTagName("AdaptationSet").item(0)
                .getAttributes().getNamedItem("lang").getNodeValue());
        assertEquals("dub", document.getElementsByTagName("Role").item(0)
                .getAttributes().getNamedItem("value").getNodeValue());
        assertEquals("48000", document.getElementsByTagName("Representation").item(0)
                .getAttributes().getNamedItem("audioSamplingRate").getNodeValue());
        assertEquals("2", document.getElementsByTagName("AudioChannelConfiguration").item(0)
                .getAttributes().getNamedItem("value").getNodeValue());
    }

    @Test
    void followsWebStreamRedirectUsingExpectedPostRequest() throws Exception {
        downloader = new StubDownloader(List.of(
                new Response(200, "OK", Map.of("Content-Type", List.of("text/plain")),
                        "https://redirect.example/video?new=1", "https://media.example/first"),
                new Response(200, "OK", Map.of("Content-Type", List.of("video/mp4")), "",
                        "https://redirect.example/video?new=1")));
        VyonEx.init(downloader);

        final Response response = YoutubeDashManifestCreatorsUtils.getInitializationResponse(
                "https://media.example/videoplayback?c=WEB", videoItag(), DeliveryType.OTF);

        assertEquals("https://redirect.example/video?new=1", response.latestUrl());
        assertEquals(2, downloader.requests.size());
        assertEquals("POST", downloader.requests.get(0).httpMethod());
        assertEquals("https://media.example/videoplayback?c=WEB&alr=yes&sq=0&rn=0",
                downloader.requests.get(0).url());
        assertEquals("https://redirect.example/video?new=1",
                downloader.requests.get(1).url());
        assertEquals("https://www.youtube.com",
                downloader.requests.get(0).headers().get("Origin").get(0));
        assertTrue(java.util.Arrays.equals(new byte[] {0x78, 0},
                downloader.requests.get(0).dataToSend()));
    }

    private static ItagItem videoItag() {
        final ItagItem itag = new ItagItem(18, ItagItem.ItagType.VIDEO,
                MediaFormat.MPEG_4, "360p", 30);
        itag.setCodec("avc1.64001E");
        itag.setBitrate(1_000_000);
        itag.setWidth(640);
        itag.setHeight(360);
        return itag;
    }

    private static final class StubDownloader extends Downloader {
        private final Queue<Response> responses;
        private final List<Request> requests = new ArrayList<>();

        private StubDownloader(final List<Response> responses) {
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public Response execute(final Request request) throws IOException, ReCaptchaException {
            requests.add(request);
            return responses.remove();
        }
    }
}
