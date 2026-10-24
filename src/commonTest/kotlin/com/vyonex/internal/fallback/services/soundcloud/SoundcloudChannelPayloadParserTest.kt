// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SoundcloudChannelPayloadParserTest {
    @Test
    fun mapsProfileAndSkipsVisualRecordsWithoutUrls() {
        val payload = SoundcloudChannelPayloadParser.parse(
            """{"username":"Alice","permalink_url":"http://soundcloud.com/alice",
                "avatar_url":"https://images.example/alice.jpg","followers_count":12,
                "track_count":4,"description":"Producer","verified":true,
                "visuals":{"visuals":[{"visual_url":"https://images.example/banner.jpg"},
                  {"visual_url":null},null,{"other":"ignored"}]}}""",
        )

        assertEquals(
            SoundcloudProfileInfoItem(
                "Alice", "http://soundcloud.com/alice", "https://images.example/alice.jpg",
                true, 12, 4, "Producer",
            ),
            payload.profile,
        )
        assertEquals(listOf("https://images.example/banner.jpg"), payload.visualUrls)
    }

    @Test
    fun rejectsInvalidUserDocuments() {
        assertFailsWith<IllegalArgumentException> { SoundcloudChannelPayloadParser.parse("[") }
        assertFailsWith<IllegalArgumentException> { SoundcloudChannelPayloadParser.parse("[]") }
    }
}
