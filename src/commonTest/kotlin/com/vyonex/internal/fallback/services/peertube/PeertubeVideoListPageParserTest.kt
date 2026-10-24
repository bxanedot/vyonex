// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeertubeVideoListPageParserTest {
    @Test
    fun parsesTotalsItemsAndApiErrors() {
        val page = PeertubeVideoListPageParser.parse(
            """{"total":13,"data":[{"video":{"uuid":"trend-1"}},null],"error":"Unavailable"}""",
        )

        assertEquals(13L, page.total)
        assertEquals("[{\"video\":{\"uuid\":\"trend-1\"}},null]", page.data.toString())
        assertEquals("Unavailable", page.error)
    }

    @Test
    fun defaultsTotalAndLeavesMissingDataForTheAdapterToReport() {
        val page = PeertubeVideoListPageParser.parse("""{"total":"13","error":4}""")

        assertEquals(0L, page.total)
        assertEquals(null, page.data)
        assertEquals(null, page.error)
    }

    @Test
    fun rejectsMalformedAndNonObjectResponses() {
        assertFailsWith<PeertubeVideoListPageParseException> {
            PeertubeVideoListPageParser.parse("not-json")
        }
        assertFailsWith<PeertubeVideoListPageParseException> {
            PeertubeVideoListPageParser.parse("[]")
        }
    }
}
