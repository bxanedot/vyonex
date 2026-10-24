// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeertubeMediaResponseParserTest {
    @Test
    fun parsesProgressiveAndPlaylistFiles() {
        assertEquals(
            PeertubeMediaPayload(
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
                ),
                playlists = listOf(
                    PeertubeMediaPlaylist(
                        playlistUrl = "https://cdn.example/master.m3u8",
                        files = listOf(
                            PeertubeMediaFile(
                                fileUrl = "https://cdn.example/audio.webm",
                                fileDownloadUrl = null,
                                torrentUrl = null,
                                resolutionId = 0,
                                resolutionLabel = "audio",
                                bitrate = 96_000,
                                hasAudio = false,
                            ),
                        ),
                    ),
                ),
                isLive = true,
            ),
            PeertubeMediaResponseParser.parse(
                """{
                    "isLive": true,
                    "files": [{
                        "fileUrl": "https://cdn.example/video.mp4",
                        "fileDownloadUrl": "https://cdn.example/download/video.mp4",
                        "torrentUrl": "https://cdn.example/video.torrent",
                        "resolution": {"id": 720, "label": "720p"},
                        "bitrate": 128000,
                        "hasAudio": true
                    }],
                    "streamingPlaylists": [{
                        "playlistUrl": "https://cdn.example/master.m3u8",
                        "files": [{
                            "fileUrl": "https://cdn.example/audio.webm",
                            "resolution": {"id": 0, "label": "audio"},
                            "bitrate": 96000,
                            "hasAudio": false
                        }]
                    }]
                }""",
            ),
        )
    }

    @Test
    fun ignoresNonObjectEntriesAndRetainsFieldDefaults() {
        assertEquals(
            PeertubeMediaPayload(
                files = listOf(PeertubeMediaFile(null, null, null, -1, null, -1, false)),
                playlists = listOf(PeertubeMediaPlaylist("https://cdn.example/live.m3u8", emptyList())),
                isLive = false,
            ),
            PeertubeMediaResponseParser.parse(
                """{
                    "isLive": "true",
                    "files": [null, 4, {
                        "fileUrl": 4,
                        "resolution": "invalid",
                        "bitrate": "96000",
                        "hasAudio": "true"
                    }],
                    "streamingPlaylists": [null, {
                        "playlistUrl": "https://cdn.example/live.m3u8",
                        "files": [false]
                    }]
                }""",
            ),
        )
    }

    @Test
    fun rejectsNonObjectVideoResponses() {
        assertFailsWith<IllegalArgumentException> {
            PeertubeMediaResponseParser.parse("[]")
        }
    }
}
