package com.vyonex
// bxane

import kotlin.test.Test
import kotlin.test.assertEquals

class VyonExPublicModelsTest {
    @Test
    fun sharedStreamAndAudioModelsRetainTheirValues() {
        val stream = VyonExStream(
            videoId = "video-id",
            url = "https://media.example/audio.m4a",
            kbps = 128,
            mimeType = "audio/mp4",
            loudnessDb = -2.0,
            clientName = "ios",
            profileId = "profile",
            headers = mapOf("User-Agent" to "VyonEx"),
        )
        val candidate = VyonExAudioCandidate(
            url = "https://media.example/audio.m4a",
            kbps = 128,
            mimeType = "audio/mp4",
            format = VyonExAudioFormat.M4A,
        )

        assertEquals("video-id", stream.videoId)
        assertEquals("VyonEx", stream.headers["User-Agent"])
        assertEquals(VyonExAudioFormat.M4A, candidate.format)
    }
}
