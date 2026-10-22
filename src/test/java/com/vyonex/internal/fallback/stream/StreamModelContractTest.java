// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.services.youtube.ItagItem;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamModelContractTest {
    @Test
    void buildsAudioStreamWithFormatAndTrackMetadata() {
        final AudioStream audio = new AudioStream.Builder()
                .setId("audio")
                .setContent("https://media.example/audio.mp3", true)
                .setMediaFormat(MediaFormat.MP3)
                .setDeliveryMethod(DeliveryMethod.HLS)
                .setAverageBitrate(128_000)
                .setAudioTrackId("en")
                .setAudioTrackName("English")
                .setAudioLocale(Locale.UK)
                .setAudioTrackType(AudioTrackType.ORIGINAL)
                .build();

        assertEquals("audio", audio.getId());
        assertEquals("https://media.example/audio.mp3", audio.getUrl());
        assertEquals(MediaFormat.MP3, audio.getFormat());
        assertEquals(DeliveryMethod.HLS, audio.getDeliveryMethod());
        assertEquals(128_000, audio.getAverageBitrate());
        assertEquals("en", audio.getAudioTrackId());
        assertEquals("English", audio.getAudioTrackName());
        assertEquals(Locale.UK, audio.getAudioLocale());
        assertEquals(AudioTrackType.ORIGINAL, audio.getAudioTrackType());
        assertEquals(Stream.ITAG_NOT_AVAILABLE_OR_NOT_APPLICABLE, audio.getItag());
    }

    @Test
    void carriesVideoMetadataFromItagAndRequiresItsCoreFields() throws Exception {
        final ItagItem itag = new ItagItem(22, ItagItem.ItagType.VIDEO,
                MediaFormat.MPEG_4, "720p");
        final VideoStream video = new VideoStream.Builder()
                .setId("video")
                .setContent("https://media.example/video.mp4", true)
                .setMediaFormat(MediaFormat.MPEG_4)
                .setDeliveryMethod(DeliveryMethod.DASH)
                .setManifestUrl("https://media.example/manifest.mpd")
                .setIsVideoOnly(true)
                .setResolution("720p")
                .setItagItem(itag)
                .build();

        assertEquals("720p", video.getResolution());
        assertTrue(video.isVideoOnly());
        assertEquals("720p", video.resolution);
        assertTrue(video.isVideoOnly);
        assertEquals(22, video.getItag());
        assertEquals(itag.getWidth(), video.getWidth());
        assertEquals(itag.getHeight(), video.getHeight());
        assertEquals("https://media.example/manifest.mpd", video.getManifestUrl());
        assertThrows(IllegalStateException.class, () -> new VideoStream.Builder().build());
        assertThrows(IllegalStateException.class,
                () -> new AudioStream.Builder().setId("audio").build());
        assertFalse(video.equalStats(null));
    }

    @Test
    void comparesStreamsWithoutFormatsByIdAndDeliveryMethod() {
        final AudioStream first = audioWithoutFormat("unknown", DeliveryMethod.HLS);
        final AudioStream same = audioWithoutFormat("unknown", DeliveryMethod.HLS);
        final AudioStream otherId = audioWithoutFormat("other", DeliveryMethod.HLS);
        final AudioStream otherDelivery = audioWithoutFormat("unknown", DeliveryMethod.DASH);

        assertTrue(first.equalStats(same));
        assertFalse(first.equalStats(otherId));
        assertFalse(first.equalStats(otherDelivery));
    }

    private static AudioStream audioWithoutFormat(final String id,
                                                  final DeliveryMethod deliveryMethod) {
        return new AudioStream.Builder().setId(id).setContent("https://media.example/audio", true)
                .setMediaFormat(null).setDeliveryMethod(deliveryMethod).build();
    }
}
