// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.services.bandcamp.BandcampService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BandcampRadioStreamExtractorTest {
    @Test
    void readsRadioMetadataAudioAndSegmentsOffline() throws Exception {
        final String body = "{\"subtitle\":\"Weekly episode\","
                + "\"image_caption\":\"<a>Host</a>\",\"published_date\":\"2024-06-15\","
                + "\"show_image_id\":12,\"desc\":\"Episode description\","
                + "\"audio_duration\":3600,\"audio_stream\":{"
                + "\"mp3-128\":\"https://audio.example/episode.mp3\","
                + "\"opus-lo\":\"https://audio.example/episode.opus\"},"
                + "\"tracks\":[{\"title\":\"Song\",\"timecode\":30,"
                + "\"track_art_id\":34,\"artist\":\"Artist\"}]}";
        final Downloader previous = VyonEx.getDownloader();
        final Downloader offline = new Downloader() {
            @Override
            public Response execute(final Request request) {
                assertEquals(BandcampExtractorHelper.BASE_API_URL + "/bcweekly/1/get?id=123",
                        request.url());
                return new Response(200, "OK", Map.of(), body, request.url());
            }
        };
        final Downloader unexpectedGlobal = new Downloader() {
            @Override
            public Response execute(final Request request) throws IOException {
                throw new IOException("Unexpected global downloader use");
            }
        };
        try {
            VyonEx.init(unexpectedGlobal);
            final BandcampRadioStreamExtractor extractor = new BandcampRadioStreamExtractor(
                    new BandcampService(1), new LinkHandler("https://bandcamp.com/?show=123",
                    "https://bandcamp.com/?show=123", "123"));
            extractor.onFetchPage(offline);

            assertEquals("Weekly episode", extractor.getName());
            assertEquals("Host", extractor.getUploaderName());
            assertEquals("2024-06-15", extractor.getTextualUploadDate());
            assertEquals("Episode description", extractor.getDescription().getContent());
            assertEquals(3600, extractor.getLength());
            assertEquals(2, extractor.getAudioStreams().size());
            assertEquals("https://audio.example/episode.mp3",
                    extractor.getAudioStreams().get(0).getContent());
            assertEquals("Song", extractor.getStreamSegments().get(0).getTitle());
            assertEquals("Artist", extractor.getStreamSegments().get(0).getChannelName());
            assertEquals("https://f4.bcbits.com/img/a34_10.jpg",
                    extractor.getStreamSegments().get(0).getPreviewUrl());
        } finally {
            VyonEx.init(previous);
        }
    }
}
