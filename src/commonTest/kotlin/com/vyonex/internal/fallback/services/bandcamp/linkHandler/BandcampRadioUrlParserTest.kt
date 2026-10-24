// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BandcampRadioUrlParserTest {
    @Test
    fun acceptsOnlyBandcampRadioShowUrls() {
        assertTrue(BandcampRadioUrlParser.accepts("https://bandcamp.com/?show=123"))
        assertTrue(BandcampRadioUrlParser.accepts("HTTP://BANDCAMP.COM?show=0#episode"))
        listOf(
            null,
            "https://example.test/?show=123",
            "ftp://bandcamp.com/?show=123",
            "https://bandcamp.com/track/song?show=123",
            "https://bandcamp.com/?show=",
            "https://bandcamp.com/?show=12&other=1",
            "https://bandcamp.com/?show=%31",
        ).forEach { assertFalse(BandcampRadioUrlParser.accepts(it), it) }
    }
}
