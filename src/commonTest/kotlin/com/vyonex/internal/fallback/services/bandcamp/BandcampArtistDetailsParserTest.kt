// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BandcampArtistDetailsParserTest {
    @Test
    fun parsesArtistAndDiscographyFields() {
        val artist = BandcampArtistDetailsParser.parse(
            """{"name":"Artist","bandcamp_url":"https://artist.bandcamp.com",
                "bio":"Artist bio","bio_image_id":5,"discography":[
                {"item_type":"album","title":"Record","band_name":"Artist",
                "band_id":10,"item_id":21,"art_id":31},null,
                {"item_type":"track","title":"Song","item_id":20,"art_id":30}]}""",
        )

        assertEquals("Artist", artist.name)
        assertEquals("https://artist.bandcamp.com", artist.url)
        assertEquals("Artist bio", artist.bio)
        assertEquals(5, artist.bioImageId)
        assertEquals(2, artist.discography.size)
        assertEquals("album", artist.discography.first().type)
        assertEquals(21, artist.discography.first().itemId)
        assertEquals(31, artist.discography.first().artId)
        assertEquals("", artist.discography.last().artist)
        assertEquals(0, artist.discography.last().artistId)
        assertEquals(20, artist.discography.last().itemId)
        assertEquals(30, artist.discography.last().artId)
    }

    @Test
    fun defaultsMissingFieldsAndReadsErrorAndBandId() {
        val artist = BandcampArtistDetailsParser.parse("""{"error":true,"discography":{}}""")

        assertTrue(artist.hasError)
        assertEquals(null, artist.name)
        assertEquals(null, artist.url)
        assertEquals("", artist.bio)
        assertEquals(0, artist.bioImageId)
        assertEquals(emptyList(), artist.discography)
        assertEquals(10, BandcampArtistDetailsParser.parseBandId("""{"id":10}"""))
        assertFailsWith<BandcampArtistDetailsParseException> {
            BandcampArtistDetailsParser.parseBandId("not-json")
        }
    }
}
