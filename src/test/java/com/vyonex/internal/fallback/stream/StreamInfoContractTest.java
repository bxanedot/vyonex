// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.services.youtube.YoutubeService;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamInfoContractTest {
    @Test
    void collectsRequiredAndOptionalStreamDataOffline() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        VyonEx.init(new EmptyDownloader());
        try {
            final YoutubeService service = new YoutubeService(7);
            final LinkHandler link = YoutubeStreamLinkHandlerFactory.getInstance().fromUrl(
                    "https://www.youtube.com/watch?v=abcdefghijk");
            final StreamInfo info = StreamInfo.getInfo(new Probe(service, link));

            assertEquals("abcdefghijk", info.getId());
            assertEquals("Sample stream", info.getName());
            assertEquals(StreamType.VIDEO_STREAM, info.getStreamType());
            assertEquals(37, info.getViewCount());
            assertEquals(1, info.getAudioStreams().size());
            assertEquals("audio", info.getAudioStreams().get(0).getId());
            assertTrue(info.getErrors().isEmpty());
        } finally {
            VyonEx.init(previous);
        }
    }

    private static final class Probe extends StreamExtractor {
        private Probe(final YoutubeService service, final LinkHandler handler) {
            super(service, handler);
        }

        @Override public void onFetchPage(final Downloader downloader) { }
        @Override public List<com.vyonex.internal.fallback.Image> getThumbnails() {
            return List.of();
        }
        @Override public String getUploaderUrl() { return ""; }
        @Override public String getUploaderName() { return "Uploader"; }
        @Override public long getViewCount() { return 37; }
        @Override public List<AudioStream> getAudioStreams() {
            return List.of(new AudioStream.Builder().setId("audio").setContent("media", true)
                    .build());
        }
        @Override public List<VideoStream> getVideoStreams() { return List.of(); }
        @Override public List<VideoStream> getVideoOnlyStreams() { return List.of(); }
        @Override public StreamType getStreamType() { return StreamType.VIDEO_STREAM; }
        @Override public String getName() { return "Sample stream"; }
    }

    private static final class EmptyDownloader extends Downloader {
        @Override public Response execute(final Request request)
                throws IOException, ReCaptchaException {
            return new Response(200, "OK", Map.of(), "", request.url());
        }
    }
}
