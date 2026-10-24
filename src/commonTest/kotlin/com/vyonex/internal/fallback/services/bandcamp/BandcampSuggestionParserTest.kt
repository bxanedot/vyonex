// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp

import kotlin.test.Test
import kotlin.test.assertEquals

class BandcampSuggestionParserTest {
    @Test
    fun returnsUniqueNonBlankNamesInResponseOrder() {
        assertEquals(
            listOf("Ambient", "Ambient dub"),
            BandcampSuggestionParser.parse(
                """{"auto":{"results":[{"name":"Ambient"},{"name":"Ambient"},{"name":"Ambient dub"},{"name":"  "},{"name":7},null]}}""",
            ),
        )
    }

    @Test
    fun missingMalformedOrUnexpectedResponseShapeReturnsNoSuggestions() {
        listOf("{", "[]", "{}", """{"auto":{"results":{}}}""").forEach { source ->
            assertEquals(emptyList(), BandcampSuggestionParser.parse(source))
        }
    }
}
