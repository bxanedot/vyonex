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

    @Test
    fun buildsResolveUrlWithFormEncodedTarget() {
        assertEquals(
            "https://api-v2.soundcloud.com/resolve?url=" +
                "https%3A%2F%2Fsoundcloud.com%2Fartist%2Ftrack%3Fa%3Done+two%26b%3D%F0%9F%90%88" +
                "&client_id=offline-key",
            SoundcloudSearchUrl.resolveUrl(
                "https://soundcloud.com/artist/track?a=one two&b=🐈",
                "offline-key",
            ),
        )
    }

    @Test
    fun buildsWidgetResolveUrlWithFormEncodedTarget() {
        assertEquals(
            "https://api-widget.soundcloud.com/resolve?url=" +
                "https%3A%2F%2Fsoundcloud.com%2Falice%3Fnext%3Da+b%26kind%3Dchannel" +
                "&format=json&client_id=offline-key",
            SoundcloudSearchUrl.widgetResolveUrl(
                "https://soundcloud.com/alice?next=a b&kind=channel",
                "offline-key",
            ),
        )
    }

    @Test
    fun addsClientIdToPaginationLinksOnlyWhenMissing() {
        assertEquals(
            "https://api-v2.soundcloud.com/users/1/tracks?client_id=key",
            SoundcloudSearchUrl.withClientId(
                "https://api-v2.soundcloud.com/users/1/tracks", "key",
            ),
        )
        assertEquals(
            "https://api-v2.soundcloud.com/users/1/tracks?offset=10&client_id=key",
            SoundcloudSearchUrl.withClientId(
                "https://api-v2.soundcloud.com/users/1/tracks?offset=10", "key",
            ),
        )
        val existing = "https://api-v2.soundcloud.com/users/1/tracks?client_id=existing"
        assertEquals(existing, SoundcloudSearchUrl.withClientId(existing, "new"))
    }
}
