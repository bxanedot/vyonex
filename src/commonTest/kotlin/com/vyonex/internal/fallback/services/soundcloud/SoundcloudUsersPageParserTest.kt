// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SoundcloudUsersPageParserTest {
    @Test
    fun mapsUserProfilesAndNextPageUrl() {
        val page = SoundcloudUsersPageParser.parse(
            """{"collection":[{"username":"Alice","permalink_url":"http://soundcloud.com/alice",
                "avatar_url":"https://images.example/alice.jpg","followers_count":12,
                "track_count":4,"description":"Producer","verified":true},null,{}],
                "next_href":"https://api-v2.soundcloud.com/users/7/followings?offset=50"}""",
        )

        assertEquals(
            listOf(
                SoundcloudProfileInfoItem(
                    "Alice", "http://soundcloud.com/alice", "https://images.example/alice.jpg",
                    true, 12, 4, "Producer",
                ),
                SoundcloudProfileInfoItem("", "", null, false, 0, 0, ""),
            ),
            page.users,
        )
        assertEquals(
            "https://api-v2.soundcloud.com/users/7/followings?offset=50",
            page.nextPageUrl,
        )
    }

    @Test
    fun rejectsInvalidUserPages() {
        assertFailsWith<IllegalArgumentException> { SoundcloudUsersPageParser.parse("{") }
        assertFailsWith<IllegalArgumentException> { SoundcloudUsersPageParser.parse("[]") }
    }
}
