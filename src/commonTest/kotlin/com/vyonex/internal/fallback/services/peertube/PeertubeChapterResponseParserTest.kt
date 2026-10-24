// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeertubeChapterResponseParserTest {
    @Test
    fun retainsValidChapterDefaultsAndNumericConversion() {
        assertEquals(
            listOf(PeertubeChapter("", 0), PeertubeChapter("Middle", 30)),
            PeertubeChapterResponseParser.parse(
                """{"chapters":[null,{"timecode":0},{"title":"Middle","timecode":30.8},{"timecode":-1},{"timecode":2147483648},{"timecode":"60"}]}""",
            ),
        )
    }

    @Test
    fun handlesMissingChaptersAndRejectsNonObjectResponses() {
        assertEquals(emptyList(), PeertubeChapterResponseParser.parse("{}"))
        assertFailsWith<IllegalArgumentException> {
            PeertubeChapterResponseParser.parse("[]")
        }
    }
}
