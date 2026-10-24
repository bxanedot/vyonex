// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SoundcloudCommentsPageParserTest {
    @Test
    fun mapsCommentFieldsAndRetainsTheNextPageUrl() {
        val page = SoundcloudCommentsPageParser.parse(
            """{"collection":[{"id":5,"body":"Great","timestamp":3500,
                "created_at":"2026-10-22T12:30:00Z","user":{"username":"Alice",
                "permalink":"alice","permalink_url":"http://soundcloud.com/alice",
                "avatar_url":"http://images.example/alice.jpg","verified":true}},null,
                {"id":null}],"next_href":"https://api-v2.soundcloud.com/comments?offset=10"}""",
        )

        assertEquals(
            listOf(
                SoundcloudCommentInfoItem(
                    "5", "Great",
                    SoundcloudProfileInfoItem(
                        "Alice", "http://soundcloud.com/alice", "http://images.example/alice.jpg",
                        true, 0, 0, "",
                    ),
                    "alice", 3500, "2026-10-22T12:30:00Z",
                ),
                SoundcloudCommentInfoItem(
                    null, "", SoundcloudProfileInfoItem("", "", null, false, 0, 0, ""),
                    "", 0, "",
                ),
            ),
            page.comments,
        )
        assertEquals("https://api-v2.soundcloud.com/comments?offset=10", page.nextPageUrl)
    }

    @Test
    fun rejectsInvalidCommentDocuments() {
        assertFailsWith<IllegalArgumentException> { SoundcloudCommentsPageParser.parse("[") }
        assertFailsWith<IllegalArgumentException> { SoundcloudCommentsPageParser.parse("[]") }
    }
}
