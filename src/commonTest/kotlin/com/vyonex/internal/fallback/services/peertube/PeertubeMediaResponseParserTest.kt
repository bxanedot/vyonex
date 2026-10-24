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
                metadata = PeertubeVideoMetadata(
                    name = "Video",
                    publishedAt = "2026-01-02T03:04:05Z",
                    description = "Description",
                    nsfw = true,
                    duration = 123,
                    views = 456,
                    likes = 7,
                    dislikes = 2,
                    accountName = "alice",
                    accountHost = "tube.example",
                    accountDisplayName = "Alice",
                    channelName = "music",
                    channelDisplayName = "Music",
                    channelUrl = "https://tube.example/c/music",
                    hlsUrl = "https://cdn.example/master.m3u8",
                    isLive = true,
                    privacyId = 1,
                    category = "Music",
                    licence = "CC BY",
                    languageId = "en",
                    tags = listOf("ambient", "instrumental"),
                    support = "Support link",
                ),
            ),
            PeertubeMediaResponseParser.parse(
                """{
                    "name": "Video",
                    "publishedAt": "2026-01-02T03:04:05Z",
                    "description": "Description",
                    "nsfw": true,
                    "duration": 123,
                    "views": 456,
                    "likes": 7,
                    "dislikes": 2,
                    "isLive": true,
                    "support": "Support link",
                    "tags": ["ambient", "instrumental", 4],
                    "account": {"name": "alice", "host": "tube.example", "displayName": "Alice"},
                    "channel": {"name": "music", "displayName": "Music", "url": "https://tube.example/c/music"},
                    "privacy": {"id": 1},
                    "category": {"label": "Music"},
                    "licence": {"label": "CC BY"},
                    "language": {"id": "en"},
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
                metadata = PeertubeVideoMetadata(
                    name = null,
                    publishedAt = null,
                    description = "",
                    nsfw = false,
                    duration = 0,
                    views = -1,
                    likes = -1,
                    dislikes = -1,
                    accountName = null,
                    accountHost = null,
                    accountDisplayName = null,
                    channelName = "",
                    channelDisplayName = null,
                    channelUrl = null,
                    hlsUrl = "https://cdn.example/live.m3u8",
                    isLive = false,
                    privacyId = -1,
                    category = "",
                    licence = "",
                    languageId = null,
                    tags = emptyList(),
                    support = "",
                ),
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

    @Test
    fun preservesLongMetadataPrecision() {
        val metadata = PeertubeMediaResponseParser
            .parse("""{"duration": 12.9, "views": 9007199254740993}""")
            .metadata

        assertEquals(12, metadata.duration)
        assertEquals(9007199254740993, metadata.views)
    }
}
