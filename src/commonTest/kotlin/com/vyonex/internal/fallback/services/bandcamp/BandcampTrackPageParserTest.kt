// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BandcampTrackPageParserTest {
    @Test
    fun parsesTrackFieldsAndSkipsNonStringOptionalMetadata() {
        val page = BandcampTrackPageParser.parse(
            """{"url":"http://artist.bandcamp.com/track/song","artist":"Artist",
                "art_id":77,"trackinfo":[{"file":{"mp3-128":"https://audio.example/song.mp3"},
                "duration":123.5}],"current":{"title":"Song","publish_date":"15 Jun 2024",
                "about":"Story","lyrics":"Words","credits":"Player","other":4}}""",
        )

        assertEquals(1, page.trackCount)
        assertTrue(page.hasFile)
        assertEquals("Song", page.title)
        assertEquals("http://artist.bandcamp.com/track/song", page.url)
        assertEquals("Artist", page.artist)
        assertEquals("15 Jun 2024", page.publishDate)
        assertEquals(77, page.artId)
        assertEquals("Story\n\nWords\n\nPlayer", page.description)
        assertEquals("https://audio.example/song.mp3", page.audioUrl)
        assertEquals(123.5, page.durationSeconds)
    }

    @Test
    fun preservesTrackCountAndDefaultsMissingOrWrongTypeFields() {
        val page = BandcampTrackPageParser.parse(
            """{"trackinfo":[{"file":null,"duration":"12"},false],"current":{"title":2}}""",
        )

        assertEquals(2, page.trackCount)
        assertFalse(page.hasFile)
        assertEquals(null, page.title)
        assertEquals(null, page.audioUrl)
        assertEquals(0L, page.artId)
        assertEquals(0.0, page.durationSeconds)
        assertEquals("", page.description)
    }

    @Test
    fun rejectsMalformedOrNonObjectPayloads() {
        assertFailsWith<BandcampTrackPageParseException> { BandcampTrackPageParser.parse("not-json") }
        assertFailsWith<BandcampTrackPageParseException> { BandcampTrackPageParser.parse("[]") }
    }
}
