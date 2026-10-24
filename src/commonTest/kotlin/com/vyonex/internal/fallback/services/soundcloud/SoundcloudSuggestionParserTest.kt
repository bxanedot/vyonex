// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SoundcloudSuggestionParserTest {
    @Test
    fun extractsStringQueriesAndIgnoresOtherCollectionEntries() {
        assertEquals(
            listOf("ambient music", ""),
            SoundcloudSuggestionParser.parse(
                """{"collection":[{"query":"ambient music"},{"query":7},null,"skip",{"query":""}]}""",
            ),
        )
    }

    @Test
    fun missingOrNonArrayCollectionIsEmpty() {
        assertEquals(emptyList(), SoundcloudSuggestionParser.parse("{}"))
        assertEquals(emptyList(), SoundcloudSuggestionParser.parse("""{"collection":{}}"""))
    }

    @Test
    fun rejectsNonObjectJson() {
        assertFailsWith<IllegalArgumentException> {
            SoundcloudSuggestionParser.parse("[]")
        }
        assertFailsWith<IllegalArgumentException> {
            SoundcloudSuggestionParser.parse("{")
        }
    }
}
