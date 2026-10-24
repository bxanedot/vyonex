// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals

class SoundcloudSearchUrlTest {
    @Test
    fun preservesSearchEndpointFiltersAndFormParameters() {
        val cases = listOf(
            listOf("tracks") to "/tracks",
            listOf("users") to "/users",
            listOf("playlists") to "/playlists",
            listOf("all") to "",
            emptyList<String>() to "",
            listOf("unknown") to "",
        )

        cases.forEach { (filters, endpoint) ->
            assertEquals(
                "https://api-v2.soundcloud.com/search$endpoint?q=ambient+%26+caf%C3%A9" +
                    "&client_id=offline-key&limit=10&offset=0",
                SoundcloudSearchUrl.build("ambient & café", filters, "offline-key", 10),
            )
        }
    }

    @Test
    fun buildsSuggestionUrlWithTheSameFormEncoding() {
        assertEquals(
            "https://api-v2.soundcloud.com/search/queries?q=ambient+%26+caf%C3%A9" +
                "&client_id=offline-key&limit=10",
            SoundcloudSearchUrl.suggestionsUrl("ambient & café", "offline-key"),
        )
    }
}
