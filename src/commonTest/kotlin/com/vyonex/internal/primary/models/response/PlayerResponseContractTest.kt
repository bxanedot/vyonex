// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.models.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerResponseContractTest {
    @Test
    fun decodesPlayerFieldsUsedByExtraction() {
        val response =
            PlayerResponse.parse(
                """
                {
                  "playabilityStatus": {"status": "OK"},
                  "streamingData": {
                    "adaptiveFormats": [
                      {"itag": 140, "mimeType": "audio/mp4", "bitrate": 128000, "contentLength": "12000", "signature_cipher": "s=encoded", "audioTrack": {"id": "en.0", "displayName": "English", "audioIsDefault": true}},
                      {"itag": 137, "mimeType": "video/mp4", "width": 1920, "height": 1080}
                    ],
                    "expiresInSeconds": 21500,
                    "hlsManifestUrl": "https://example.test/hls",
                    "dashManifestUrl": "https://example.test/dash",
                    "serverAbrStreamingUrl": "https://example.test/sabr"
                  },
                  "videoDetails": {"videoId": "abcdefghijk", "title": "Title", "author": "Artist", "channelId": "channel", "lengthSeconds": "120", "musicVideoType": "MUSIC_VIDEO_TYPE_ATV", "viewCount": "1234", "isLiveContent": false, "thumbnail": {"thumbnails": [{"url": "https://example.test/thumb", "width": 120, "height": 90}]}},
                  "playbackTracking": {"videostatsPlaybackUrl": {"baseUrl": "https://example.test/playback"}, "videostatsWatchtimeUrl": {"baseUrl": "https://example.test/watchtime"}, "atrUrl": {"baseUrl": "https://example.test/atr"}, "videostatsScheduledFlushWalltimeSeconds": [5, 15], "videostatsDefaultFlushIntervalSeconds": 5},
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
        assertEquals(12_000L, formats[0].contentLength)
        assertEquals("en.0", formats[0].audioTrack?.id)
        assertEquals(true, formats[0].audioTrack?.audioIsDefault)
        assertFalse(formats[1].isAudio)
        assertEquals("abcdefghijk", response.videoDetails?.videoId)
        assertEquals("Artist", response.videoDetails?.author)
        assertEquals("https://example.test/thumb", response.videoDetails?.thumbnail?.thumbnails?.single()?.url)
        assertEquals(-4.25, response.playerConfig?.audioConfig?.loudnessDb)
        assertEquals("opaque", response.playerConfig?.mediaCommonConfig?.mediaUstreamerRequestConfig?.videoPlaybackUstreamerConfig)
        assertEquals(21500, streamingData.expiresInSeconds)
        assertEquals("https://example.test/hls", streamingData.hlsManifestUrl)
        assertEquals(listOf(5, 15), response.playbackTracking?.videostatsScheduledFlushWalltimeSeconds)
        assertEquals("https://example.test/playback", response.playbackTracking?.videostatsPlaybackUrl?.baseUrl)
    }

    @Test
    fun appliesProtocolDefaultsAndIgnoresUnconsumedFields() {
        val response =
            PlayerResponse.parse(
                """{"playabilityStatus":{"status":"OK"},"streamingData":{"adaptiveFormats":[{}]},"unrecognized":true}""",
            )

        val format = requireNotNull(response.streamingData).adaptiveFormats.single()
        assertEquals(-1, format.itag)
        assertEquals("", format.mimeType)
        assertEquals(0, format.bitrate)
        assertTrue(format.isAudio)
    }

    @Test
    fun rejectsPlayerResponsesWithoutRequiredPlayabilityStatus() {
        assertFailsWith<IllegalStateException> {
            PlayerResponse.parse("""{"playabilityStatus":{}}""")
        }
    }
}
