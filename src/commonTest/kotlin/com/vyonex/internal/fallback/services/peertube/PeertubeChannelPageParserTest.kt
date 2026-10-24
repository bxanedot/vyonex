// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeertubeChannelPageParserTest {
    @Test
    fun parsesChannelAndOwnerMetadataAndImages() {
        val channel = PeertubeChannelPageParser.parse(
            """{"id":9,"displayName":"Music","description":"Independent music",
                "followersCount":12,"avatar":{"path":"/channel.png"},
                "banner":{"path":"/banner.jpg"},"ownerAccount":{"name":"alice",
                "url":"https://tube.example/a/alice","avatar":{"path":"/alice.png"}}}""",
        )

        assertEquals("9", channel.id)
        assertEquals("Music", channel.displayName)
        assertEquals("Independent music", channel.description)
        assertEquals(12L, channel.followerCount)
        assertEquals("alice", channel.ownerName)
        assertEquals("https://tube.example/a/alice", channel.ownerUrl)
        assertEquals(listOf(PeertubeImageReference("/channel.png")), channel.avatars)
        assertEquals(listOf(PeertubeImageReference("/banner.jpg")), channel.banners)
        assertEquals(listOf(PeertubeImageReference("/alice.png")), channel.ownerAvatars)
        assertEquals(null, channel.error)
    }

    @Test
    fun keepsApiErrorsAndDefaultsAbsentFields() {
        val channel = PeertubeChannelPageParser.parse("""{"error":"Channel unavailable"}""")

        assertEquals("null", channel.id)
        assertEquals(0L, channel.followerCount)
        assertEquals("Channel unavailable", channel.error)
        assertEquals(emptyList(), channel.avatars)
    }

    @Test
    fun rejectsMalformedOrNonObjectResponses() {
        assertFailsWith<PeertubeChannelPageParseException> {
            PeertubeChannelPageParser.parse("not-json")
        }
        assertFailsWith<PeertubeChannelPageParseException> {
            PeertubeChannelPageParser.parse("[]")
        }
    }
}
