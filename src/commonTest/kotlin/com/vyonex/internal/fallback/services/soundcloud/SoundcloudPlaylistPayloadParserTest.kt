// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SoundcloudPlaylistPayloadParserTest {
    @Test
    fun separatesEmbeddedTracksFromCompactIdsAndMapsPlaylistMetadata() {
        val payload = SoundcloudPlaylistPayloadParser.parse(
            """{"title":"Mix","permalink_url":"http://soundcloud.com/a/sets/mix",
                "description":"Night set","track_count":2,
                "user":{"username":"Alice","permalink_url":"http://soundcloud.com/alice",
                  "avatar_url":"http://images.example/alice.jpg","verified":true},
                "tracks":[
                  {"id":42,"title":"Intro","duration":125000,"playback_count":8,
                   "created_at":"2026-10-24T00:00:00Z","artwork_url":"https://images.example/intro.jpg",
                   "user":{"username":"Alice","permalink_url":"http://soundcloud.com/alice"}},
                  {"id":9007199254740993},{"id":null},null]}""",
        )

        assertEquals(
            SoundcloudPlaylistInfoItem(
                "Mix",
                "http://soundcloud.com/a/sets/mix",
                SoundcloudProfileInfoItem(
                    "Alice", "http://soundcloud.com/alice", "http://images.example/alice.jpg",
                    true, 0, 0, "",
                ),
                2,
                "https://images.example/intro.jpg",
            ),
            payload.playlist,
        )
        assertEquals("Night set", payload.description)
        assertEquals(
            listOf(
                SoundcloudTrackInfoItem(
                    "42", "", "Intro", 125_000,
                    SoundcloudProfileInfoItem("Alice", "http://soundcloud.com/alice", null,
                        false, 0, 0, ""),
                    "2026-10-24T00:00:00Z", 8, "https://images.example/intro.jpg",
                ),
            ),
            payload.initialTracks,
        )
        assertEquals(listOf("9007199254740993"), payload.unresolvedTrackIds)
    }

    @Test
    fun parsesBatchTracksAndRejectsInvalidPayloads() {
        assertEquals(
            listOf("9007199254740993", "2", ""),
            SoundcloudPlaylistTrackBatchParser.parse(
                """[{"id":9007199254740993,"title":"First"},null,{"id":2,"title":"Second"},{"id":null,"title":"No ID"}]""",
            ).map { it.id },
        )
        assertFailsWith<IllegalArgumentException> { SoundcloudPlaylistPayloadParser.parse("[") }
        assertFailsWith<IllegalArgumentException> { SoundcloudPlaylistTrackBatchParser.parse("{}") }
    }
}
