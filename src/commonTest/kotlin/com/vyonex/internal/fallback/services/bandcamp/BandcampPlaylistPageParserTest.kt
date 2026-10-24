// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BandcampPlaylistPageParserTest {
    @Test
    fun readsAlbumMetadataAndTrackList() {
        val album = BandcampPlaylistPageParser.parseTracks(
            """{"url":"https://artist.bandcamp.com/album/record","artist":"Artist",
                "art_id":50,"trackinfo":[{"title":"Song","title_link":"/track/song",
                "duration":123.9},null,{"title":false,"duration":"5"}]}""",
        )

        assertEquals("https://artist.bandcamp.com/album/record", album.url)
        assertEquals("Artist", album.artist)
        assertEquals(50, album.artId)
        assertEquals(3, album.trackCount)
        assertEquals(2, album.tracks.size)
        assertEquals("Song", album.tracks.first().title)
        assertEquals("/track/song", album.tracks.first().titleLink)
        assertEquals(123, album.tracks.first().duration)
        assertEquals("", album.tracks.last().artist)
        assertEquals("Record", BandcampPlaylistPageParser.parseTitle("""{"album_title":"Record"}"""))
    }

    @Test
    fun defaultsMissingFieldsAndRejectsMalformedObjects() {
        val album = BandcampPlaylistPageParser.parseTracks("{}")

        assertEquals(null, album.url)
        assertEquals("", album.artist)
        assertEquals(0, album.artId)
        assertEquals(0, album.trackCount)
        assertEquals(emptyList(), album.tracks)
        assertEquals(null, BandcampPlaylistPageParser.parseTitle("{}"))
        assertFailsWith<BandcampPlaylistPageParseException> {
            BandcampPlaylistPageParser.parseTracks("not-json")
        }
        assertFailsWith<BandcampPlaylistPageParseException> {
            BandcampPlaylistPageParser.parseTitle("[]")
        }
    }
}
