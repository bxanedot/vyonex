// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals

class PeertubeMediaMapperTest {
    @Test
    fun mapsProgressiveVariantsTorrentsAndAudioByResolution() {
        val streams = PeertubeMediaMapper.map(
            files = listOf(
                PeertubeMediaFile(
                    fileUrl = "https://cdn.example/video.mp4",
                    fileDownloadUrl = "https://cdn.example/download/video.mp4",
                    torrentUrl = "https://cdn.example/video.torrent",
                    resolutionId = 720,
                    resolutionLabel = "720p",
                    bitrate = 128_000,
                    hasAudio = true,
                ),
                PeertubeMediaFile(
                    fileUrl = "https://cdn.example/audio.webm?token=1",
                    fileDownloadUrl = "https://cdn.example/audio.webm?token=1",
                    torrentUrl = null,
                    resolutionId = 0,
                    resolutionLabel = "audio",
                    bitrate = 96_000,
                    hasAudio = false,
                ),
            ),
            playlists = emptyList(),
            isLive = false,
        )

        assertEquals(listOf("audio-webm"), streams.audio.map { it.id })
        assertEquals("webm", streams.audio.single().formatSuffix)
        assertEquals(
            listOf("720p-mp4", "720p-mp4-fileDownloadUrl", "720p-torrent"),
            streams.video.map { it.id },
        )
        assertEquals(PeertubeMediaDelivery.TORRENT, streams.video.last().delivery)
    }

    @Test
    fun mapsHlsFilesAndAddsManifestFallbackOnlyWhenNoFilesArePlayable() {
        val streams = PeertubeMediaMapper.map(
            files = emptyList(),
            playlists = listOf(
                PeertubeMediaPlaylist(
                    playlistUrl = "https://cdn.example/master.m3u8",
                    files = listOf(
                        PeertubeMediaFile(
                            fileUrl = "https://cdn.example/720.mp4",
                            fileDownloadUrl = null,
                            torrentUrl = null,
                            resolutionId = 720,
                            resolutionLabel = "720p",
                            bitrate = 0,
                            hasAudio = true,
                        ),
                    ),
                ),
                PeertubeMediaPlaylist(
                    playlistUrl = "https://cdn.example/live.m3u8",
                    files = listOf(PeertubeMediaFile(null, null, null, 0, null, 0, false)),
                ),
            ),
            isLive = true,
        )

        assertEquals(listOf("720p-hls", "live-hls"), streams.video.map { it.id })
        assertEquals("https://cdn.example/master.m3u8", streams.video.first().manifestUrl)
        assertEquals(PeertubeMediaDelivery.HLS, streams.video.first().delivery)
    }

    @Test
    fun ignoresUnplayableEntriesAndUsesLiveManifestFallback() {
        val streams = PeertubeMediaMapper.map(
            files = listOf(PeertubeMediaFile(" ", null, null, 0, "audio", 0, false)),
            playlists = listOf(PeertubeMediaPlaylist("https://cdn.example/live.m3u8", emptyList())),
            isLive = true,
        )

        assertEquals(emptyList(), streams.audio)
        assertEquals(listOf("live-hls"), streams.video.map { it.id })
    }
}
