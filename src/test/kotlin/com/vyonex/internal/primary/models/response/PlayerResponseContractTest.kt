// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.models.response

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerResponseContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesPlayerFieldsUsedByExtraction() {
        val response =
            json.decodeFromString<PlayerResponse>(
                """
                {
                  "playabilityStatus": {"status": "OK"},
                  "streamingData": {
                    "adaptiveFormats": [
                      {"itag": 140, "mimeType": "audio/mp4", "bitrate": 128000, "signature_cipher": "s=encoded"},
                      {"itag": 137, "mimeType": "video/mp4", "width": 1920, "height": 1080}
                    ],
                    "expiresInSeconds": 21500,
                    "serverAbrStreamingUrl": "https://example.test/sabr"
                  },
                  "videoDetails": {"videoId": "abcdefghijk", "isLiveContent": false},
                  "playerConfig": {
                    "audioConfig": {"loudnessDb": -4.25, "perceptualLoudnessDb": -3.75},
                    "mediaCommonConfig": {
                      "mediaUstreamerRequestConfig": {"videoPlaybackUstreamerConfig": "opaque"}
                    }
                  }
                }
                """.trimIndent(),
            )

        val streamingData = requireNotNull(response.streamingData)
        val formats = streamingData.adaptiveFormats
        assertEquals("OK", response.playabilityStatus.status)
        assertEquals(2, formats.size)
        assertTrue(formats[0].isAudio)
        assertEquals("s=encoded", formats[0].signatureCipher)
        assertFalse(formats[1].isAudio)
        assertEquals("abcdefghijk", response.videoDetails?.videoId)
        assertEquals(-4.25, response.playerConfig?.audioConfig?.loudnessDb)
        assertEquals("opaque", response.playerConfig?.mediaCommonConfig?.mediaUstreamerRequestConfig?.videoPlaybackUstreamerConfig)
        assertEquals(21500, streamingData.expiresInSeconds)
    }
}
