// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.AudioTrackType;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItagItemTest {
    @Test
    void derivesFormatMetadataFromTheStreamResponse() throws Exception {
        final ItagItem video = ItagItem.fromStreamFormat(JsonParser.object().from(
                "{\"itag\":18,\"mimeType\":\"video/mp4; codecs=\\\"avc1\\\"\","
                        + "\"qualityLabel\":\"720p60\",\"fps\":60}"),
                ItagItem.ItagType.VIDEO);
        assertEquals(18, video.id);
        assertEquals(MediaFormat.MPEG_4, video.getMediaFormat());
        assertEquals("720p60", video.getResolutionString());
        assertEquals(60, video.getFps());

        final ItagItem audio = ItagItem.fromStreamFormat(JsonParser.object().from(
                "{\"itag\":140,\"mimeType\":\"audio/mp4; codecs=\\\"mp4a.40.2\\\"\","
                        + "\"averageBitrate\":128000}"), ItagItem.ItagType.AUDIO);
        assertEquals(MediaFormat.M4A, audio.getMediaFormat());
        assertEquals(128000, audio.getAverageBitrate());
    }

    @Test
    void rejectsFormatsWithoutAMappedMediaType() {
        assertThrows(ParsingException.class, () -> ItagItem.fromStreamFormat(
                JsonParser.object().from("{\"itag\":99,\"mimeType\":\"application/x-unknown\"}"),
                ItagItem.ItagType.VIDEO));
    }

    @Test
    void copyKeepsAllStreamMetadata() {
        final ItagItem source = new ItagItem(18, ItagItem.ItagType.VIDEO,
                MediaFormat.MPEG_4, "720p60", 60);
        source.setBitrate(1_000_000);
        source.setWidth(1280);
        source.setHeight(720);
        source.setInitStart(1);
        source.setInitEnd(2);
        source.setIndexStart(3);
        source.setIndexEnd(4);
        source.setQuality("hd720");
        source.setCodec("avc1");
        source.setTargetDurationSec(5);
        source.setApproxDurationMs(6_000);
        source.setContentLength(7_000);
        source.setAudioTrackId("fr.1");
        source.setAudioTrackName("French");
        source.setAudioTrackType(AudioTrackType.DUBBED);
        source.setAudioLocale(Locale.FRENCH);
        source.setIsDrc(true);
        source.setLastModified(8);
        source.setXtags("tags");

        final ItagItem copy = new ItagItem(source);

        assertEquals(source.getMediaFormat(), copy.getMediaFormat());
        assertEquals(source.getBitrate(), copy.getBitrate());
        assertEquals(source.getResolutionString(), copy.getResolutionString());
        assertEquals(source.getAudioTrackType(), copy.getAudioTrackType());
        assertEquals(source.getAudioLocale(), copy.getAudioLocale());
        assertEquals(source.isDrc(), copy.isDrc());
        assertEquals(source.getLastModified(), copy.getLastModified());
        assertEquals(source.getXtags(), copy.getXtags());
    }
}
