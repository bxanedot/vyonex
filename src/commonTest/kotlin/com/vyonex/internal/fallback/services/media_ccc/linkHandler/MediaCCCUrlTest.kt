// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc.linkHandler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaCCCUrlTest {
    @Test
    fun normalizesSupportedUrlsForSharedRouting() {
        val parsed = MediaCCCUrl.parse("MEDIA.CCC.DE/public/events/talk%2Done?lang=de")

        assertEquals("https", parsed?.scheme)
        assertEquals("media.ccc.de", parsed?.host)
        assertEquals("/public/events/talk-one", parsed?.path)
        assertTrue(parsed?.hasQuery == true)
    }

    @Test
    fun rejectsUnsupportedAndMalformedUrls() {
        assertNull(MediaCCCUrl.parse(null))
        assertNull(MediaCCCUrl.parse("  "))
        assertNull(MediaCCCUrl.parse("ftp://media.ccc.de/v/talk"))
        assertFalse(MediaCCCUrl.hasHost(
            MediaCCCUrl.parse("https://media.ccc.de.evil/v/talk")!!,
            "media.ccc.de",
        ))
        assertNull(MediaCCCUrl.parse("https://media.ccc.de/%zz"))
    }

    @Test
    fun queryAndPathSegmentsKeepRouteSemantics() {
        val parsed = MediaCCCUrl.parse("https://streaming.media.ccc.de/chaos-2026/room")

        assertEquals(listOf("chaos-2026", "room"), MediaCCCUrl.pathSegments(parsed!!))
        assertFalse(parsed.hasQuery)
    }
}
