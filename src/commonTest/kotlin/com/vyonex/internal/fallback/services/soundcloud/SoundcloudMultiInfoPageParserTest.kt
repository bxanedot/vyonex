// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SoundcloudMultiInfoPageParserTest {
    @Test
    fun mapsChannelTabItemsAndLikes() {
        val page = SoundcloudMultiInfoPageParser.parse(
            """{"collection":[{"kind":"user","username":"Alice"},
                {"kind":"like","track":{"id":9,"title":"Liked"}},
                {"kind":"like","playlist":{"title":"Mix"}},
                {"kind":"unknown"},null],
                "next_href":"https://api-v2.soundcloud.com/users/7/likes?offset=20"}""",
        )

        assertEquals(
            listOf(
                SoundcloudSearchResult.User(
                    SoundcloudProfileInfoItem("Alice", "", null, false, 0, 0, ""),
                ),
                SoundcloudSearchResult.Track(
                    SoundcloudTrackInfoItem("9", "", "Liked", 0,
                        SoundcloudProfileInfoItem("", "", null, false, 0, 0, ""),
                        "", 0, null),
                ),
                SoundcloudSearchResult.Playlist(
                    SoundcloudPlaylistInfoItem("Mix", "",
                        SoundcloudProfileInfoItem("", "", null, false, 0, 0, ""),
                        0, null),
                ),
            ),
            page.items,
        )
        assertEquals("https://api-v2.soundcloud.com/users/7/likes?offset=20", page.nextPageUrl)
    }

    @Test
    fun rejectsInvalidItemPages() {
        assertFailsWith<IllegalArgumentException> { SoundcloudMultiInfoPageParser.parse("{") }
        assertFailsWith<IllegalArgumentException> { SoundcloudMultiInfoPageParser.parse("[]") }
    }
}
