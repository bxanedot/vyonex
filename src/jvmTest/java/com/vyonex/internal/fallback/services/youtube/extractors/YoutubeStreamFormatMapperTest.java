// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.services.youtube.ItagItem;
import com.vyonex.internal.fallback.stream.AudioStream;
import com.vyonex.internal.fallback.stream.DeliveryMethod;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.stream.VideoStream;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeStreamFormatMapperTest {
    @Test
    void mapsAudioFormatMetadataAndAppendsEscapedPlaybackParameters() throws Exception {
        final ItagInfo stream = YoutubeStreamFormatMapper.map(
                "video-id",
                JsonParser.object().from("{\"itag\":140,"
                        + "\"mimeType\":\"audio/mp4; codecs=\\\"mp4a.40.2\\\"\","
                        + "\"averageBitrate\":128000,\"bitrate\":130000,"
                        + "\"audioSampleRate\":\"48000\",\"audioChannels\":1,"
                        + "\"audioTrack\":{\"id\":\"fr.1\",\"displayName\":\"French\"},"
                        + "\"contentLength\":\"456\",\"approxDurationMs\":\"12345\","
                        + "\"url\":\"https://media.example/play?x=1#frag\"}"),
                ItagItem.ItagType.AUDIO, StreamType.VIDEO_STREAM, "nonce value", "token+value");

        assertEquals("https://media.example/play?x=1&cpn=nonce+value&pot=token%2Bvalue#frag",
                stream.getContent());
        assertTrue(stream.getIsUrl());
        assertEquals(140, stream.getItagItem().id);
        assertEquals(48000, stream.getItagItem().getSampleRate());
        assertEquals(1, stream.getItagItem().getAudioChannels());
        assertEquals("French", stream.getItagItem().getAudioTrackName());
        assertEquals("fr", stream.getItagItem().getAudioLocale().getLanguage());
        assertEquals(456, stream.getItagItem().getContentLength());
        assertEquals(12345, stream.getItagItem().getApproxDurationMs());

        final AudioStream built = YoutubeStreamFormatMapper.audioStream(stream,
                StreamType.VIDEO_STREAM);
        assertEquals("140", built.getId());
        assertEquals(DeliveryMethod.PROGRESSIVE_HTTP, built.getDeliveryMethod());
    }

    @Test
    void marksPostLiveFormatsAsNonUrlAndKeepsByteRanges() throws Exception {
        final ItagInfo stream = YoutubeStreamFormatMapper.map(
                "video-id",
                JsonParser.object().from("{\"itag\":18,"
                        + "\"mimeType\":\"video/mp4; codecs=\\\"avc1\\\"\","
                        + "\"qualityLabel\":\"360p\",\"fps\":30,\"bitrate\":900000,"
                        + "\"initRange\":{\"start\":\"0\",\"end\":\"99\"},"
                        + "\"indexRange\":{\"start\":\"100\",\"end\":\"199\"},"
                        + "\"targetDurationSec\":5,\"type\":\"FORMAT_STREAM_TYPE_OTF\","
                        + "\"url\":\"https://media.example/play\"}"),
                ItagItem.ItagType.VIDEO, StreamType.POST_LIVE_STREAM, "nonce", null);

        assertFalse(stream.getIsUrl());
        assertEquals(0, stream.getItagItem().getInitStart());
        assertEquals(99, stream.getItagItem().getInitEnd());
        assertEquals(100, stream.getItagItem().getIndexStart());
        assertEquals(199, stream.getItagItem().getIndexEnd());
        assertEquals(5, stream.getItagItem().getTargetDurationSec());
        assertEquals(30, stream.getItagItem().getFps());

        final VideoStream built = YoutubeStreamFormatMapper.videoStream(stream,
                StreamType.POST_LIVE_STREAM, true);
        assertTrue(built.isVideoOnly());
        assertEquals(DeliveryMethod.DASH, built.getDeliveryMethod());
    }

    @Test
    void skipsFormatsWithoutAUsableUrl() throws Exception {
        assertNull(YoutubeStreamFormatMapper.map(
                "video-id",
                JsonParser.object().from("{\"itag\":140,"
                        + "\"mimeType\":\"audio/mp4\",\"signatureCipher\":\"itag=140\"}"),
                ItagItem.ItagType.AUDIO, StreamType.VIDEO_STREAM, "nonce", null));
        assertNull(YoutubeStreamFormatMapper.map(
                "video-id",
                JsonParser.object().from("{\"itag\":140,"
                        + "\"mimeType\":\"audio/mp4\",\"url\":\" \"}"),
                ItagItem.ItagType.AUDIO, StreamType.VIDEO_STREAM, "nonce", null));
    }

    @Test
    void keepsTheFirstOfSimilarStreamsAcrossClientResponses() throws Exception {
        final ItagInfo first = YoutubeStreamFormatMapper.map("video-id",
                JsonParser.object().from("{\"itag\":18,"
                        + "\"mimeType\":\"video/mp4\",\"qualityLabel\":\"360p\","
                        + "\"url\":\"https://media.example/first\"}"),
                ItagItem.ItagType.VIDEO, StreamType.VIDEO_STREAM, "nonce-one", null);
        final ItagInfo second = YoutubeStreamFormatMapper.map("video-id",
                JsonParser.object().from("{\"itag\":18,"
                        + "\"mimeType\":\"video/mp4\",\"qualityLabel\":\"360p\","
                        + "\"url\":\"https://media.example/second\"}"),
                ItagItem.ItagType.VIDEO, StreamType.VIDEO_STREAM, "nonce-two", null);
        final List<VideoStream> streams = new ArrayList<>();

        YoutubeStreamExtractor.appendDistinctStreams(streams,
                java.util.stream.Stream.of(first, second),
                info -> YoutubeStreamFormatMapper.videoStream(
                        info, StreamType.VIDEO_STREAM, false));

        assertEquals(1, streams.size());
        assertEquals(first.getContent(), streams.get(0).getContent());
    }
}
