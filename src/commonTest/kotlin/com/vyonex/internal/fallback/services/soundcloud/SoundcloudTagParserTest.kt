// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals

class SoundcloudTagParserTest {
    @Test
    fun splitsWhitespaceAndKeepsQuotedAndEscapedContent() {
        assertEquals(
            listOf("ambient", "chill beats", "say \"hello\" now", "raw\\path"),
            SoundcloudTagParser.parse("ambient\t\"chill beats\" \"say \\\"hello\\\" now\" raw\\\\path"),
        )
    }

    @Test
    fun handlesEmptyAndUnterminatedTagLists() {
        assertEquals(emptyList(), SoundcloudTagParser.parse(null))
        assertEquals(emptyList(), SoundcloudTagParser.parse(" \t "))
        assertEquals(listOf("unfinished phrase"), SoundcloudTagParser.parse("\"unfinished phrase"))
    }
}
