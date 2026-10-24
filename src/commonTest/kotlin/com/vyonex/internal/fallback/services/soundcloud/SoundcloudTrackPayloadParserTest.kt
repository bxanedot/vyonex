// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SoundcloudTrackPayloadParserTest {
    @Test
    fun parsesResolvedIdsWithoutCoercingNullOrStructuredValues() {
        assertEquals("42", SoundcloudTrackPayloadParser.parseId("""{"id":42}"""))
        assertEquals("track", SoundcloudTrackPayloadParser.parseId("""{"id":"track"}"""))
        assertEquals("{\"value\":1}", SoundcloudTrackPayloadParser.parseId("""{"id":{"value":1}}"""))
        assertEquals(null, SoundcloudTrackPayloadParser.parseId("{}"))
        assertEquals(null, SoundcloudTrackPayloadParser.parseId("""{"id":null}"""))
        assertFailsWith<IllegalArgumentException> { SoundcloudTrackPayloadParser.parseId("[]") }
    }

    @Test
    fun mapsTrackMetadataAndTranscodings() {
        assertEquals(
            SoundcloudTrackPayload(
                policy = "ALLOW",
                id = "42",
                title = "Song",
                createdAt = "2026-10-22T12:30:00Z",
                artworkUrl = "https://cdn.example/artwork.jpg",
                description = "A track",
                durationMillis = 125_000,
                playbackCount = 9_007_199_254_740_993,
                likesCount = 12,
                userPermalinkUrl = "https://soundcloud.com/alice",
                userName = "Alice",
                userAvatarUrl = "https://cdn.example/avatar.jpg",
                userVerified = true,
                streamable = true,
                trackAuthorization = "token",
                sharing = "public",
                genre = "Ambient",
                license = "cc-by",
                tagList = "ambient \"chill beats\"",
                transcodings = listOf(
                    SoundcloudTranscoding("https://api.example/stream", "progressive", "mp3_0_0"),
                ),
            ),
            SoundcloudTrackPayloadParser.parse(
                """{"id":42,"title":"Song","policy":"ALLOW","created_at":"2026-10-22T12:30:00Z","artwork_url":"https://cdn.example/artwork.jpg","description":"A track","duration":125000,"playback_count":9007199254740993,"likes_count":12,"user":{"permalink_url":"https://soundcloud.com/alice","username":"Alice","avatar_url":"https://cdn.example/avatar.jpg","verified":true},"streamable":true,"track_authorization":"token","sharing":"public","genre":"Ambient","license":"cc-by","tag_list":"ambient \"chill beats\"","media":{"transcodings":[null,{"url":"https://api.example/stream","format":{"protocol":"progressive"},"preset":"mp3_0_0"}]}}""",
            ),
        )
    }

    @Test
    fun retainsMetadataDefaultsAndArtworkFallback() {
        assertEquals(
            SoundcloudTrackPayload(
                policy = "",
                id = "null",
                title = "",
                createdAt = null,
                artworkUrl = "https://cdn.example/avatar.jpg",
                description = "",
                durationMillis = 0,
                playbackCount = 0,
                likesCount = -1,
                userPermalinkUrl = "",
                userName = "",
                userAvatarUrl = "https://cdn.example/avatar.jpg",
                userVerified = false,
                streamable = false,
                trackAuthorization = null,
                sharing = "",
                genre = "",
                license = "",
                tagList = "",
                transcodings = emptyList(),
            ),
            SoundcloudTrackPayloadParser.parse(
                """{"title":4,"duration":"125000","likes_count":"12","artwork_url":null,"user":{"avatar_url":"https://cdn.example/avatar.jpg","verified":"true"},"streamable":"true","media":{"transcodings":[false]}}""",
            ),
        )
    }

    @Test
    fun rejectsNonObjectTrackResponses() {
        assertFailsWith<IllegalArgumentException> {
            SoundcloudTrackPayloadParser.parse("[]")
        }
    }
}
