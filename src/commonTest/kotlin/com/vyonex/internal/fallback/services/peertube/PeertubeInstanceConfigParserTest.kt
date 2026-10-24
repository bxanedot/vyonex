// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeertubeInstanceConfigParserTest {
    @Test
    fun readsTheInstanceNameAndTreatsOtherShapesAsMissing() {
        assertEquals("VyonEx Tube", PeertubeInstanceConfigParser.parse(
            """{"instance":{"name":"VyonEx Tube"}}""",
        ))
        assertEquals(null, PeertubeInstanceConfigParser.parse("{}"))
        assertEquals(null, PeertubeInstanceConfigParser.parse("""{"instance":{"name":4}}"""))
    }

    @Test
    fun rejectsMalformedOrNonObjectConfiguration() {
        assertFailsWith<PeertubeInstanceConfigParseException> {
            PeertubeInstanceConfigParser.parse("not-json")
        }
        assertFailsWith<PeertubeInstanceConfigParseException> {
            PeertubeInstanceConfigParser.parse("[]")
        }
    }
}
