// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BandcampUrlParserTest {
    @Test
    fun parsesHostDecodedPathAndRawQuery() {
        assertEquals(
            BandcampUrlParser.Parsed("artist.bandcamp.com", "/track/song title", "show=12&x=a%26b"),
            BandcampUrlParser.parse("https://artist.bandcamp.com/track/song%20title?show=12&x=a%26b"),
        )
    }

    @Test
    fun preservesEmptyQueryAndDropsQueryTextFromFragment() {
        assertEquals("", BandcampUrlParser.parse("https://bandcamp.com/?")?.rawQuery)
        assertNull(BandcampUrlParser.parse("https://bandcamp.com/#?show=12")?.rawQuery)
    }

    @Test
    fun decodesEscapedPathSeparatorsBeforeClassifyingSegments() {
        assertEquals(
            listOf("track", "song", "live"),
            BandcampUrlParser.parse("https://artist.bandcamp.com/track/song%2Flive")?.segments,
        )
    }

    @Test
    fun classifiesArtistPagesAndContentPaths() {
        fun parse(value: String) = requireNotNull(BandcampUrlParser.parse(value))

        assertTrue(BandcampUrlParser.isArtistPage(parse("https://artist.bandcamp.com/")))
        assertTrue(BandcampUrlParser.isArtistPage(parse("https://artist.bandcamp.com/MUSIC")))
        assertFalse(BandcampUrlParser.isArtistPage(parse("https://artist.bandcamp.com/track/song")))
        assertFalse(BandcampUrlParser.isArtistPage(parse("https://daily.bandcamp.com/")))
        assertTrue(BandcampUrlParser.isArtistContent(parse("https://artist.bandcamp.com/Track/song"), "track"))
        assertFalse(BandcampUrlParser.isArtistContent(parse("https://artist.bandcamp.com/track"), "track"))
    }

    @Test
    fun rejectsInvalidWebUrls() {
        listOf(
            null,
            "",
            "artist.bandcamp.com/track/song",
            "ftp://artist.bandcamp.com/track/song",
            "https:///track/song",
            "https://artist.bandcamp.com/track/%",
            "https://artist.bandcamp.com/track/song title",
            "https://artist.bandcamp.com\\track\\song",
        ).forEach { assertNull(BandcampUrlParser.parse(it), it) }
    }
}
