// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class YoutubeSuggestionParserTest {
    @Test
    fun keepsNonblankSuggestionStringsAndSkipsOtherRows() {
        assertEquals(
            listOf("Ambient", "Ambient dub", "Ambient"),
            YoutubeSuggestionParser.parse(
                """["query",[["Ambient",1],["   ",2],["Ambient dub",3],{},["Ambient",4],[]]]""",
            ),
        )
    }

    @Test
    fun rejectsMalformedOrUnexpectedResponseShapes() {
        listOf("not-json", "{}", "[]", "[\"query\",{}]").forEach { source ->
            assertFailsWith<YoutubeSuggestionParseException> {
                YoutubeSuggestionParser.parse(source)
            }
        }
    }
}
