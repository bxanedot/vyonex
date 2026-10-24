// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.exceptions.ContentNotSupportedException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.services.bandcamp.BandcampService;
import com.vyonex.internal.fallback.stream.StreamType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
                + "\"track_art_id\":34,\"artist\":\"Artist\"},"
                + "{\"title\":\" \"},\"ignored\"]}";
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
            assertEquals("https://bandcamp.com/?show=123", extractor.getUrl());
            assertEquals("Host", extractor.getUploaderName());
            assertThrows(ContentNotSupportedException.class, extractor::getUploaderUrl);
            assertEquals("2024-06-15", extractor.getTextualUploadDate());
            assertEquals("2024-06-15T00:00:00Z",
                    extractor.getUploadDate().getInstant().toString());
            assertEquals("Episode description", extractor.getDescription().getContent());
            assertEquals(3600, extractor.getLength());
            assertEquals(2, extractor.getAudioStreams().size());
            assertEquals(MediaFormat.MP3, extractor.getAudioStreams().get(0).getFormat());
            assertEquals(MediaFormat.OPUS, extractor.getAudioStreams().get(1).getFormat());
            assertEquals("https://audio.example/episode.mp3",
                    extractor.getAudioStreams().get(0).getContent());
            assertEquals("https://audio.example/episode.opus",
                    extractor.getAudioStreams().get(1).getContent());
            assertEquals(StreamType.AUDIO_STREAM, extractor.getStreamType());
            assertEquals("https://f4.bcbits.com/img/12_10.jpg",
                    extractor.getThumbnails().get(0).getUrl());
            assertEquals("https://bandcamp.com/img/buttons/bandcamp-button-circle-whitecolor-512.png",
                    extractor.getUploaderAvatars().get(0).getUrl());
            assertEquals(1, extractor.getStreamSegments().size());
            assertEquals("Song", extractor.getStreamSegments().get(0).getTitle());
            assertEquals(30, extractor.getStreamSegments().get(0).getStartTimeSeconds());
            assertEquals("Artist", extractor.getStreamSegments().get(0).getChannelName());
            assertEquals("https://f4.bcbits.com/img/a34_10.jpg",
                    extractor.getStreamSegments().get(0).getPreviewUrl());
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void rejectsFailedAndMalformedShowResponses() {
        final Downloader previous = VyonEx.getDownloader();
        try {
            for (final Response response : List.of(
                    new Response(503, "Unavailable", Map.of(), "{}", "https://bandcamp.com/api"),
                    new Response(200, "OK", Map.of(), "not json", "https://bandcamp.com/api"))) {
                final Downloader offline = new Downloader() {
                    @Override
                    public Response execute(final Request request) {
                        return response;
                    }
                };
                VyonEx.init(offline);
                final BandcampRadioStreamExtractor extractor = new BandcampRadioStreamExtractor(
                        new BandcampService(1), new LinkHandler("https://bandcamp.com/?show=123",
                        "https://bandcamp.com/?show=123", "123"));

                assertThrows(ParsingException.class, () -> extractor.onFetchPage(offline));
            }
        } finally {
            VyonEx.init(previous);
        }
    }
}
