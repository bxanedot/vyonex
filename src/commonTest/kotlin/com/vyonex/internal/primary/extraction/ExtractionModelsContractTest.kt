// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ExtractionModelsContractTest {
    @Test
    fun hintsCopyPreservesEntitlementAndStreamCapabilities() {
        val hints =
            ContentHints(isLive = true)
                .withPremium()
                .withStreamCapabilities(allowHls = false, allowSabr = false)
                .copy(maxVideoHeight = 720)

        assertEquals(true, hints.isLive)
        assertEquals(720, hints.maxVideoHeight)
        assertEquals(true, hints.premium)
        assertEquals(false, hints.allowHls)
        assertEquals(false, hints.allowSabr)
        assertEquals(true, hints.allowBoundedRange)
    }

    @Test
    fun extractionDiagnosticsRedactUrlsAndCredentials() {
        val config = PlayerConfig("https://private.test/player?key=secret", 123, "visitor-secret", "v1")
        val playback = PlaybackTrackingData("nonce-secret", "https://private.test/playback", null, null, null, 1)
        val stream =
            ExtractedStream(
                videoId = "abcdefghijk",
                audioUrl = "https://private.test/audio?signature=secret",
                headers = mapOf("Authorization" to "bearer-secret"),
                loudnessDb = null,
                expiresAt = null,
                contentLengthBytes = null,
                itag = 140,
                mimeType = "audio/mp4",
                codecs = null,
                bitrate = null,
                sampleRate = null,
                clientName = "test",
                profileId = "test",
                requireBoundedRange = false,
                rangeChunkSizeBytes = 0,
                playbackTracking = playback,
            )

        listOf(config.toString(), playback.toString(), stream.toString()).forEach { summary ->
            assertFalse(summary.contains("private.test"))
            assertFalse(summary.contains("secret"))
            assertFalse(summary.contains("bearer-secret"))
        }
    }
}
