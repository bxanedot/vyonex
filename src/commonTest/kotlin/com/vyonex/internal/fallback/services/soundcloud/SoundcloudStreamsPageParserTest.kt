// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SoundcloudStreamsPageParserTest {
    @Test
    fun mapsTrackAndChartPagesWithoutReordering() {
        val tracks = SoundcloudStreamsPageParser.parse(
            """{"collection":[{"id":42,"title":"Rain","permalink_url":"https://soundcloud.com/a/rain",
                "duration":90000,"user":{"username":"Alice"}},null],
                "next_href":"https://api-v2.soundcloud.com/tracks?offset=10"} """,
        )
        val charts = SoundcloudStreamsPageParser.parse(
            """{"collection":[{"track":{"id":7,"title":"Wind"}},{}]}""",
            charts = true,
        )

        assertEquals(listOf("42"), tracks.tracks.map { it.id })
        assertEquals("Rain", tracks.tracks.single().title)
        assertEquals("https://api-v2.soundcloud.com/tracks?offset=10", tracks.nextPageUrl)
        assertEquals(listOf("7", ""), charts.tracks.map { it.id })
        assertEquals(listOf("Wind", ""), charts.tracks.map { it.title })
    }

    @Test
    fun rejectsInvalidTrackPages() {
        assertFailsWith<IllegalArgumentException> { SoundcloudStreamsPageParser.parse("{") }
        assertFailsWith<IllegalArgumentException> { SoundcloudStreamsPageParser.parse("[]") }
    }
}
