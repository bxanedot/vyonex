// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

import com.vyonex.internal.primary.models.response.PlayerResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SabrBootstrapFactoryContractTest {
    @Test
    fun mapsPlaybackResponseIntoBoundedBootstrap() {
        val audio =
            PlayerResponse.StreamingData.Format(
                itag = 140,
                mimeType = "audio/webm; codecs=opus",
                contentLength = 5_000,
                approxDurationMs = "15000",
                lastModified = "1234",
                audioTrack = PlayerResponse.StreamingData.Format.AudioTrack(id = "en.0"),
                isDrc = true,
            )
        val discard = video(itag = 137, height = 360, bitrate = 400_000, lastModified = "4321")
        val selected = video(itag = 248, height = 720, bitrate = 1_500_000, lastModified = "9876")
        val response = response(listOf(audio, discard, selected))

        val bootstrap =
            response.toSabrBootstrap(
                clientId = 67,
                clientVersion = "1.0",
                audioFormat = audio,
                poToken = "Y2hhbGxlbmdl",
                requestUserAgent = "VyonEx test agent",
                requestOrigin = "https://music.youtube.com",
                videoFormat = selected,
            )

        assertEquals("abcdefghijk", bootstrap.videoId)
        assertEquals("https://r1.googlevideo.com/videoplayback?expire=1", bootstrap.serverAbrStreamingUrl)
        assertEquals("abc", bootstrap.videoPlaybackUstreamerConfig.decodeToString())
        assertEquals("challenge", bootstrap.poToken?.decodeToString())
        assertEquals(SabrFormatId(140, 1234), bootstrap.audioFormat)
        assertEquals(SabrFormatId(137, 4321), bootstrap.discardVideoFormat)
        assertEquals(360, bootstrap.discardVideoHeight)
        assertEquals(SabrFormatId(248, 9876), bootstrap.selectedVideoFormat)
        assertEquals(720, bootstrap.selectedVideoHeight)
        assertEquals(15_000, bootstrap.durationMs)
        assertEquals("audio/webm", bootstrap.mimeType)
        assertEquals("en.0", bootstrap.audioTrackId)
        assertEquals(true, bootstrap.isDrc)
    }

    @Test
    fun acceptsOnlySecurePlaybackEndpointsAndKnownOriginProfiles() {
        assertEquals(
            "https://r1.googlevideo.com/videoplayback?x=1",
            requireAllowedSabrUrl("https://r1.googlevideo.com/videoplayback?x=1"),
        )
        listOf(
            "http://r1.googlevideo.com/videoplayback",
            "https://googlevideo.com.evil.test/videoplayback",
            "https://r1.googlevideo.com/other",
            "https://user@r1.googlevideo.com/videoplayback",
            "https://r1.googlevideo.com:444/videoplayback",
        ).forEach { url -> assertThrows(SabrProtocolException::class.java) { requireAllowedSabrUrl(url) } }
        assertEquals("https://www.youtube.com", sabrRequestOrigin("WEB"))
        assertEquals("https://music.youtube.com", sabrRequestOrigin("WEB_REMIX"))
        assertNull(sabrRequestOrigin("ANDROID"))
    }

    @Test
    fun rejectsMissingFormatIdentityAndOversizedConfiguration() {
        val response = response(listOf(video(137, 360, 400_000, "4321")))
        val audioWithoutIdentity = PlayerResponse.StreamingData.Format(itag = 140, mimeType = "audio/mp4", approxDurationMs = "1")

        assertThrows(IllegalArgumentException::class.java) {
            response.toSabrBootstrap(1, "1", audioWithoutIdentity)
        }
        assertThrows(SabrProtocolException::class.java) {
            response(
                listOf(video(137, 360, 400_000, "4321")),
                configuration = "A".repeat(100_000),
            ).toSabrBootstrap(
                clientId = 1,
                clientVersion = "1",
                audioFormat = PlayerResponse.StreamingData.Format(itag = 140, lastModified = "1", approxDurationMs = "1"),
            )
        }
    }

    @Test
    fun usesVideoDurationWhenAudioDurationIsMissingAndRejectsOverflow() {
        val response = response(listOf(video(137, 360, 400_000, "4321")))
        val audio = PlayerResponse.StreamingData.Format(itag = 140, mimeType = "audio/mp4", lastModified = "1")

        assertEquals(20_000, response.toSabrBootstrap(1, "1", audio).durationMs)
        val overflow = response.copy(videoDetails = response.videoDetails?.copy(lengthSeconds = "9223372036854776"))
        assertThrows(IllegalStateException::class.java) { overflow.toSabrBootstrap(1, "1", audio) }
    }

    private fun response(formats: List<PlayerResponse.StreamingData.Format>, configuration: String = "YWJj") =
        PlayerResponse(
            playabilityStatus = PlayerResponse.PlayabilityStatus("OK"),
            streamingData =
                PlayerResponse.StreamingData(
                    adaptiveFormats = formats,
                    serverAbrStreamingUrl = "https://r1.googlevideo.com/videoplayback?expire=1",
                ),
            videoDetails = PlayerResponse.VideoDetails(videoId = "abcdefghijk", lengthSeconds = "20"),
            playerConfig =
                PlayerResponse.PlayerConfig(
                    mediaCommonConfig =
                        PlayerResponse.PlayerConfig.MediaCommonConfig(
                            mediaUstreamerRequestConfig =
                                PlayerResponse.PlayerConfig.MediaCommonConfig.MediaUstreamerRequestConfig(configuration),
                        ),
                ),
        )

    private fun video(itag: Int, height: Int, bitrate: Int, lastModified: String) =
        PlayerResponse.StreamingData.Format(
            itag = itag,
            mimeType = "video/mp4; codecs=avc1",
            bitrate = bitrate,
            width = height * 16 / 9,
            height = height,
            lastModified = lastModified,
        )

}
