// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

import kotlin.test.Test
import kotlin.test.assertEquals

class SabrBootstrapContractTest {
    @Test
    fun bootstrapByteArraysUseContentEquality() {
        val bootstrap =
            SabrBootstrap(
                videoId = "abcdefghijk",
                serverAbrStreamingUrl = "https://private.test/sabr",
                videoPlaybackUstreamerConfig = byteArrayOf(1, 2),
                clientName = 1,
                clientVersion = "1",
                audioFormat = SabrFormatId(140),
                discardVideoFormat = SabrFormatId(0),
                discardVideoHeight = 0,
                durationMs = 1000,
                contentLengthBytes = null,
                mimeType = "audio/mp4",
                poToken = byteArrayOf(3, 4),
            )

        assertEquals(bootstrap, bootstrap.copy(videoPlaybackUstreamerConfig = byteArrayOf(1, 2), poToken = byteArrayOf(3, 4)))
        assertEquals(bootstrap.hashCode(), bootstrap.copy(videoPlaybackUstreamerConfig = byteArrayOf(1, 2), poToken = byteArrayOf(3, 4)).hashCode())
    }
}
