// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SoundcloudStreamUrlParserTest {
    @Test
    fun readsOptionalStreamUrl() {
        assertEquals(
            "https://cdn.example/stream.mp3",
            SoundcloudStreamUrlParser.parse("""{"url":"https://cdn.example/stream.mp3"}"""),
        )
        assertEquals(null, SoundcloudStreamUrlParser.parse("{}"))
        assertEquals(null, SoundcloudStreamUrlParser.parse("""{"url":4}"""))
    }

    @Test
    fun rejectsMalformedOrNonObjectResponses() {
        assertFailsWith<SoundcloudStreamUrlParseException> {
            SoundcloudStreamUrlParser.parse("not-json")
        }
        assertFailsWith<SoundcloudStreamUrlParseException> {
            SoundcloudStreamUrlParser.parse("[]")
        }
    }
}
