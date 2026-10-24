// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import com.vyonex.internal.primary.models.response.PlayerResponse.StreamingData.Format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormatSelectorsContractTest {
    @Test
    fun `audio selection follows quality mode and url requirement`() {
        val opus = audio(itag = 1, mimeType = "audio/webm", bitrate = 96_000)
        val aacLow = audio(itag = 2, mimeType = "audio/mp4", bitrate = 64_000)
        val aacHigh = audio(itag = 3, mimeType = "audio/mp4", bitrate = 128_000)
        val missingUrl = audio(itag = 4, mimeType = "audio/webm", bitrate = 192_000, url = null)

        assertEquals(opus, selectBestAudioFormat(listOf(aacHigh, opus, aacLow)))
        assertEquals(aacLow, selectBestAudioFormat(listOf(aacHigh, opus, aacLow), AudioQuality.LOW))
        assertEquals(aacHigh, selectBestAudioFormat(listOf(opus, aacHigh), AudioQuality.HIGH))
        assertEquals(aacHigh, selectBestAudioFormat(listOf(opus, aacHigh), AudioQuality.MP4))
        assertNull(selectBestAudioFormat(listOf(opus), AudioQuality.MP4))
        assertEquals(missingUrl, selectBestAudioFormat(listOf(missingUrl), requireUrl = false))
        assertNull(selectBestAudioFormat(listOf(missingUrl)))
    }

    @Test
    fun `high audio mode uses bitrate then channel sample rate and container`() {
        val mono = audio(itag = 1, bitrate = 128_000, channels = 1, sampleRate = 48_000)
        val stereo = audio(itag = 2, bitrate = 128_000, channels = 2, sampleRate = 44_100)
        val higherRate = audio(itag = 3, bitrate = 128_000, channels = 2, sampleRate = 48_000)
        val webm = audio(itag = 4, mimeType = "audio/webm", bitrate = 128_000, channels = 2, sampleRate = 48_000)

        assertEquals(webm, selectBestAudioFormat(listOf(mono, stereo, higherRate, webm), AudioQuality.HIGH))
    }

    @Test
    fun `video selection respects cap then chooses smallest available resolution`() {
        val low = video(itag = 1, height = 720, mimeType = "video/mp4; codecs=avc1", bitrate = 2_000_000)
        val highBitrate = video(itag = 2, height = 1080, mimeType = "video/mp4; codecs=avc1", bitrate = 8_000_000)
        val lowBitrate = video(itag = 3, height = 1080, mimeType = "video/mp4; codecs=avc1", bitrate = 4_000_000)
        val av1 = video(itag = 4, height = 1080, mimeType = "video/mp4; codecs=av01", bitrate = 1_000_000)
        val taller = video(itag = 5, height = 1440, mimeType = "video/mp4; codecs=avc1", bitrate = 3_000_000)

        assertEquals(lowBitrate, selectBestVideoFormat(listOf(low, highBitrate, lowBitrate, av1, taller), maxHeight = 1080))
        assertEquals(taller, selectBestVideoFormat(listOf(taller, video(6, 2160)), maxHeight = 1080))
    }

    @Test
    fun `video selection ignores incomplete formats and requires urls by default`() {
        val withoutUrl = video(itag = 1, height = 720, url = null)
        val withoutWidth = Format(itag = 2, url = "https://example.test/video", mimeType = "video/mp4", height = 1080)
        val invalidHeight = video(itag = 3, height = 0)

        assertNull(selectBestVideoFormat(listOf(withoutUrl, withoutWidth, invalidHeight)))
        assertEquals(withoutUrl, selectBestVideoFormat(listOf(withoutUrl), requireUrl = false))
    }

    private fun audio(
        itag: Int,
        mimeType: String = "audio/mp4",
        bitrate: Int,
        channels: Int? = 2,
        sampleRate: Int? = 48_000,
        url: String? = "https://example.test/audio",
    ): Format =
        Format(
            itag = itag,
            url = url,
            mimeType = mimeType,
            bitrate = bitrate,
            audioChannels = channels,
            audioSampleRate = sampleRate,
        )

    private fun video(
        itag: Int,
        height: Int,
        mimeType: String = "video/mp4",
        bitrate: Int = 2_000_000,
        url: String? = "https://example.test/video",
    ): Format =
        Format(
            itag = itag,
            url = url,
            mimeType = mimeType,
            bitrate = bitrate,
            width = height * 16 / 9,
            height = height,
        )
}
