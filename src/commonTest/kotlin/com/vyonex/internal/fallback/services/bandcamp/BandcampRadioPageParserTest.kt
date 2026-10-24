// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BandcampRadioPageParserTest {
    @Test
    fun parsesRadioShowsAndSkipsNonObjectResults() {
        val shows = BandcampRadioPageParser.parse(
            """{"results":[{"id":321,"subtitle":"Weekly episode","title":"Bandcamp Weekly",
                "desc":"About the show","date":"15 Jun 2024 12:30:00 UTC","image_id":55},null]}""",
        )

        assertEquals(
            listOf(BandcampRadioShow(
                321, "Weekly episode", "Bandcamp Weekly", "About the show",
                "15 Jun 2024 12:30:00 UTC", 55,
            )),
            shows,
        )
    }

    @Test
    fun appliesLegacyDefaultsAndRejectsMalformedOrMissingResults() {
        assertEquals(
            BandcampRadioShow(0, null, "", null, null, 0),
            BandcampRadioPageParser.parse("""{"results":[{}]}""").single(),
        )
        assertFailsWith<BandcampRadioPageParseException> {
            BandcampRadioPageParser.parse("not-json")
        }
        assertFailsWith<BandcampRadioResultsMissingException> {
            BandcampRadioPageParser.parse("{}")
        }
    }
}
