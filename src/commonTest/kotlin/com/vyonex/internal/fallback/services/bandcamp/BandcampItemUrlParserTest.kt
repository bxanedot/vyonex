// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BandcampItemUrlParserTest {
    @Test
    fun readsTheStringUrlFromTheResponse() {
        assertEquals("http://artist.bandcamp.com/album/record", BandcampItemUrlParser.parse(
            """{"bandcamp_url":"http://artist.bandcamp.com/album/record"}""",
        ))
    }

    @Test
    fun missingOrNonStringUrlsAreAbsent() {
        assertEquals(null, BandcampItemUrlParser.parse("{}"))
        assertEquals(null, BandcampItemUrlParser.parse("""{"bandcamp_url":42}"""))
    }

    @Test
    fun rejectsMalformedOrNonObjectResponses() {
        assertFailsWith<BandcampItemUrlParseException> { BandcampItemUrlParser.parse("not-json") }
        assertFailsWith<BandcampItemUrlParseException> { BandcampItemUrlParser.parse("[]") }
    }
}
