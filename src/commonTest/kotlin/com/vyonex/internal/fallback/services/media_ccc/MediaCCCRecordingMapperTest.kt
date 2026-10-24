// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc

import kotlin.test.Test
import kotlin.test.assertEquals

class MediaCCCRecordingMapperTest {
    @Test
    fun mapsSupportedAndUnknownFormatsWithoutDroppingRecognizedMediaKinds() {
        val streams = MediaCCCRecordingMapper.map(
            listOf(
                MediaCCCRecordingPayload("audio-opus", "/audio.opus", "audio/opus", "de", null),
                MediaCCCRecordingPayload("audio-mp3", "/audio.mp3", "audio/mpeg", null, null),
                MediaCCCRecordingPayload("audio-unknown", "/audio.wav", "audio/wav", null, null),
                MediaCCCRecordingPayload("video-webm", "/video.webm", "video/webm", null, 720),
                MediaCCCRecordingPayload("video-mp4", "/video.mp4", "video/mp4", null, 1080),
                MediaCCCRecordingPayload("video-unknown", "/video.mkv", "video/x-matroska", null, null),
                MediaCCCRecordingPayload("ignored", "/other", "application/octet-stream", null, null),
            ),
        )

        assertEquals(listOf("opus", "mp3", null), streams.audio.map { it.formatSuffix })
        assertEquals(listOf("webm", "mp4", null), streams.video.map { it.formatSuffix })
        assertEquals("de", streams.audio.first().language)
        assertEquals(1080, streams.video[1].height)
    }

    @Test
    fun missingRecordingsProduceEmptyLists() {
        assertEquals(MediaCCCRecordingStreams(emptyList(), emptyList()), MediaCCCRecordingMapper.map(null))
    }
}
