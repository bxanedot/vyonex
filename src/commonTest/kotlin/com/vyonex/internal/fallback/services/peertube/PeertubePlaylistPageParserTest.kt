// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeertubePlaylistPageParserTest {
    @Test
    fun parsesPlaylistMetadataAndImages() {
        val playlist = PeertubePlaylistPageParser.parse(
            """{"displayName":"Mix","description":"Playlist notes","videosLength":13,
                "thumbnailPath":"/mix.jpg","previewPath":"/preview.jpg",
                "ownerAccount":{"displayName":"Alice","url":"https://tube.example/a/alice",
                "avatar":{"path":"/alice.png"}},"videoChannel":{"displayName":"Music",
                "url":"https://tube.example/c/music","avatar":{"path":"/music.png"}}}""",
        )

        assertEquals("Mix", playlist.displayName)
        assertEquals("Playlist notes", playlist.description)
        assertEquals(13L, playlist.videoCount)
        assertEquals("Alice", playlist.ownerName)
        assertEquals("https://tube.example/a/alice", playlist.ownerUrl)
        assertEquals("Music", playlist.channelName)
        assertEquals("https://tube.example/c/music", playlist.channelUrl)
        assertEquals(
            listOf(
                PeertubeImageReference("/mix.jpg", size = PeertubeImageSize.LOW),
                PeertubeImageReference("/preview.jpg", size = PeertubeImageSize.MEDIUM),
            ),
            playlist.thumbnails,
        )
        assertEquals(listOf(PeertubeImageReference("/alice.png")), playlist.ownerAvatars)
        assertEquals(listOf(PeertubeImageReference("/music.png")), playlist.channelAvatars)
        assertEquals(null, playlist.error)
    }

    @Test
    fun preservesApiErrorsAndDefaultsMissingFields() {
        val playlist = PeertubePlaylistPageParser.parse("""{"error":"Playlist unavailable"}""")

        assertEquals(null, playlist.displayName)
        assertEquals(-1L, playlist.videoCount)
        assertEquals("Playlist unavailable", playlist.error)
        assertEquals(emptyList(), playlist.thumbnails)
    }

    @Test
    fun rejectsMalformedOrNonObjectResponses() {
        assertFailsWith<PeertubePlaylistPageParseException> {
            PeertubePlaylistPageParser.parse("not-json")
        }
        assertFailsWith<PeertubePlaylistPageParseException> {
            PeertubePlaylistPageParser.parse("[]")
        }
    }

    @Test
    fun requiresOwnerAndChannelObjectsForAvailablePlaylists() {
        assertFailsWith<PeertubePlaylistPageParseException> {
            PeertubePlaylistPageParser.parse("""{"displayName":"Mix","videoChannel":{}}""")
        }
        assertFailsWith<PeertubePlaylistPageParseException> {
            PeertubePlaylistPageParser.parse(
                """{"displayName":"Mix","ownerAccount":{},"videoChannel":[]}""",
            )
        }
    }
}
