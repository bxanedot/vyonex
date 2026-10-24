// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals

class SoundcloudTranscodingMapperTest {
    @Test
    fun `maps supported presets and delivery protocols`() {
        val candidates = SoundcloudTranscodingMapper.map(
            true,
            listOf(
                SoundcloudTranscoding("https://api.example/mp3", "progressive", "mp3_0_0"),
                SoundcloudTranscoding("https://api.example/opus", "hls", "opus_0_0"),
                SoundcloudTranscoding("https://api.example/aac", "progressive", "aac_160k"),
            ),
        )
        assertEquals(
            listOf(
                SoundcloudAudioCandidate(
                    "https://api.example/mp3", "mp3_0_0", SoundcloudAudioFormat.MP3, false,
                ),
                SoundcloudAudioCandidate(
                    "https://api.example/opus", "opus_0_0", SoundcloudAudioFormat.OPUS, true,
                ),
                SoundcloudAudioCandidate(
                    "https://api.example/aac", "aac_160k", SoundcloudAudioFormat.M4A, false,
                ),
            ),
            candidates,
        )
        assertEquals(listOf(128, 64, 160), candidates.map { it.format.bitrate })
    }

    @Test
    fun `ignores unavailable endpoints encrypted and unsupported transcodings`() {
        assertEquals(
            emptyList(),
            SoundcloudTranscodingMapper.map(
                true,
                listOf(
                    SoundcloudTranscoding(null, "progressive", "mp3_0_0"),
                    SoundcloudTranscoding(" ", "progressive", "mp3_0_0"),
                    SoundcloudTranscoding("https://api.example/encrypted", "encrypted_hls", "mp3_0_0"),
                    SoundcloudTranscoding("https://api.example/unknown", "progressive", "flac"),
                ),
            ),
        )
    }

    @Test
    fun `does not map transcodings for non-streamable tracks`() {
        assertEquals(
            emptyList(),
            SoundcloudTranscodingMapper.map(
                false,
                listOf(SoundcloudTranscoding("https://api.example/mp3", "progressive", "mp3_0_0")),
            ),
        )
    }
}
