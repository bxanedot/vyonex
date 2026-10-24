// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeertubeAccountPageParserTest {
    @Test
    fun parsesAccountMetadataAndImages() {
        val account = PeertubeAccountPageParser.parse(
            """{"id":7,"displayName":"Alice","description":"Producer","followersCount":5,
                "avatar":{"path":"/alice.png"},"banner":{"path":"/account-banner.jpg"}}""",
        )

        assertEquals("7", account.id)
        assertEquals("Alice", account.displayName)
        assertEquals("Producer", account.description)
        assertEquals(5L, account.followerCount)
        assertEquals(listOf(PeertubeImageReference("/alice.png")), account.avatars)
        assertEquals(listOf(PeertubeImageReference("/account-banner.jpg")), account.banners)
        assertEquals(null, account.error)
    }

    @Test
    fun preservesApiErrorsAndDefaultsMissingFields() {
        val account = PeertubeAccountPageParser.parse("""{"error":"Account unavailable"}""")

        assertEquals("null", account.id)
        assertEquals(0L, account.followerCount)
        assertEquals("Account unavailable", account.error)
        assertEquals(emptyList(), account.avatars)
        assertEquals(emptyList(), account.banners)
    }

    @Test
    fun rejectsMalformedOrNonObjectResponses() {
        assertFailsWith<PeertubeAccountPageParseException> {
            PeertubeAccountPageParser.parse("not-json")
        }
        assertFailsWith<PeertubeAccountPageParseException> {
            PeertubeAccountPageParser.parse("[]")
        }
    }
}
