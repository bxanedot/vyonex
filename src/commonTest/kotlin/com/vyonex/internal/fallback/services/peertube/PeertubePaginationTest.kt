// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PeertubePaginationTest {
    @Test
    fun advancesOffsetAndPreservesOtherUrlParts() {
        assertEquals(
            "https://tube.example/api/videos?filter=a%26b&start=24&count=12#page",
            PeertubePagination.nextPageUrl(
                "https://tube.example/api/videos?filter=a%26b&start=%31%32&count=12#page",
                30,
            ),
        )
    }

    @Test
    fun returnsNoNextPageWhenOffsetIsMissingInvalidOrExhausted() {
        val base = "https://tube.example/api/videos"
        assertNull(PeertubePagination.nextPageUrl("$base?count=12", 100))
        assertNull(PeertubePagination.nextPageUrl("$base?start=bad", 100))
        assertNull(PeertubePagination.nextPageUrl("$base?start=%", 100))
        assertNull(PeertubePagination.nextPageUrl("$base?start=12", 24))
        assertNull(PeertubePagination.nextPageUrl("$base?start=-1", 100))
        assertNull(
            PeertubePagination.nextPageUrl("$base?start=9223372036854775807", Long.MAX_VALUE),
        )
    }
}
