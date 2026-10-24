// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SoundcloudSearchPageParserTest {
    @Test
    fun mapsSupportedResultKindsAndKeepsPaginationMetadata() {
        val page = SoundcloudSearchPageParser.parse(
            """{"collection":[
                {"kind":"track","title":"Rain","permalink_url":"https://soundcloud.com/a/rain",
                 "duration":92000,"created_at":"2026-10-24T00:00:00Z","playback_count":7,
                 "user":{"username":"Alice","permalink_url":"https://soundcloud.com/alice",
                   "avatar_url":"https://img.example/alice.jpg","verified":true}},
                {"kind":"user","username":"Bob","followers_count":8,"track_count":3},
                {"kind":"playlist","title":"Mix","track_count":2,
                 "tracks":[null,{"user":{"avatar_url":"https://img.example/playlist.jpg"}}]},
                {"kind":"unsupported"}, null],
                "next_href":"https://api-v2.soundcloud.com/search?offset=10"}""",
        )

        assertEquals(5, page.collectionSize)
        assertEquals(
            SoundcloudSearchResult.Track(
                SoundcloudTrackInfoItem(
                    "", "https://soundcloud.com/a/rain", "Rain", 92_000,
                    SoundcloudProfileInfoItem(
                        "Alice", "https://soundcloud.com/alice", "https://img.example/alice.jpg",
                        true, 0, 0, "",
                    ),
                    "2026-10-24T00:00:00Z", 7, "https://img.example/alice.jpg",
                ),
            ),
            page.results[0],
        )
        assertEquals(
            SoundcloudSearchResult.User(
                SoundcloudProfileInfoItem("Bob", "", null, false, 8, 3, ""),
            ),
            page.results[1],
        )
        assertEquals(
            SoundcloudSearchResult.Playlist(
                SoundcloudPlaylistInfoItem(
                    "Mix", "", SoundcloudProfileInfoItem("", "", null, false, 0, 0, ""),
                    2, "https://img.example/playlist.jpg",
                ),
            ),
            page.results[2],
        )
        assertEquals("https://api-v2.soundcloud.com/search?offset=10", page.nextPageUrl)
    }

    @Test
    fun preservesEmptyCollectionAndRejectsInvalidDocuments() {
        assertEquals(0, SoundcloudSearchPageParser.parse("{}").collectionSize)
        assertFailsWith<IllegalArgumentException> { SoundcloudSearchPageParser.parse("[") }
        assertFailsWith<IllegalArgumentException> { SoundcloudSearchPageParser.parse("[]") }
    }
}
