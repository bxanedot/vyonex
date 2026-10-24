// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PeertubeFieldReaderTest {
    @Test
    fun readsNestedValuesWithoutCoercingTypes() {
        val item = mapOf(
            "account" to mapOf("name" to "Creator", "followers" to 12L, "stringNumber" to "12"),
        )

        assertEquals("Creator", PeertubeFieldReader.stringAt(item, "account.name"))
        assertEquals(12L, PeertubeFieldReader.numberAt(item, "account.followers"))
        assertNull(PeertubeFieldReader.numberAt(item, "account.stringNumber"))
        assertNull(PeertubeFieldReader.stringAt(item, "account.missing"))
    }
}
