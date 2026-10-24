// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeertubeUrlTest {
    @Test
    fun parsesHttpUrlsWithoutDecodingRouteSegments() {
        assertEquals(
            PeertubeUrl.Parsed("/videos/watch/a%2Fb", "start=1&filter=x%26y"),
            PeertubeUrl.parse("https://tube.example/videos/watch/a%2Fb?start=1&filter=x%26y"),
        )
        assertEquals(
            listOf("videos", "watch", "a%2Fb"),
            PeertubeUrl.segments("http://tube.example/videos/watch/a%2Fb"),
        )
    }

    @Test
    fun acceptsOnlyAbsoluteHttpUrlsWithoutCredentialsOrMalformedEscapes() {
        assertTrue(PeertubeUrl.accepts("https://tube.example/w/video-id"))
        assertNull(PeertubeUrl.parse("tube.example/w/video-id"))
        assertNull(PeertubeUrl.parse("ftp://tube.example/w/video-id"))
        assertNull(PeertubeUrl.parse("https://user@tube.example/w/video-id"))
        assertNull(PeertubeUrl.parse("https://tube.example/w/%"))
        assertNull(PeertubeUrl.parse("https://tube.example/a b"))
        assertNull(PeertubeUrl.parse("https://tube.example\\w/video-id"))
        assertFalse(PeertubeUrl.accepts("https:///w/video-id"))
    }

    @Test
    fun extractsVideoIdsFromSupportedWatchRoutes() {
        listOf(
            "/w/video-id" to "video-id",
            "/videos/video-id" to "video-id",
            "/videos/watch/video-id" to "video-id",
            "/videos/embed/video-id" to "video-id",
        ).forEach { (path, id) ->
            assertEquals(id, PeertubeUrl.videoId(requireNotNull(PeertubeUrl.parse("https://tube.example$path"))))
        }
        assertNull(PeertubeUrl.videoId(requireNotNull(PeertubeUrl.parse(
            "https://tube.example/videos/watch/playlist/list-id",
        ))))
    }

    @Test
    fun extractsPlaylistIdsFromSupportedRoutes() {
        listOf(
            "/w/p/list-id",
            "/videos/watch/playlist/list-id",
            "/api/v1/video-playlists/list-id",
        ).forEach { path ->
            assertEquals("list-id", PeertubeUrl.playlistId(
                requireNotNull(PeertubeUrl.parse("https://tube.example$path")),
            ))
        }
        assertNull(PeertubeUrl.playlistId(requireNotNull(
            PeertubeUrl.parse("https://tube.example/videos/watch/video-id"),
        )))
    }

    @Test
    fun canonicalizesChannelAliasesAndApiRoutes() {
        listOf(
            "/accounts/alice/videos" to "accounts/alice",
            "/a/alice/videos" to "accounts/alice",
            "/video-channels/creator/video-playlists" to "video-channels/creator",
            "/c/creator/video-playlists" to "video-channels/creator",
            "/api/v1/accounts/alice" to "accounts/alice",
        ).forEach { (path, id) ->
            assertEquals(id, PeertubeUrl.channelId(
                requireNotNull(PeertubeUrl.parse("https://tube.example$path")),
            ))
        }
        assertNull(PeertubeUrl.channelId(requireNotNull(
            PeertubeUrl.parse("https://tube.example/api/v1/accounts"),
        )))
    }

    @Test
    fun mapsChannelTabsToTheirRouteSuffixes() {
        assertEquals("/videos", PeertubeUrl.channelTabSuffix("videos"))
        assertEquals("/video-channels", PeertubeUrl.channelTabSuffix("channels"))
        assertEquals("/video-playlists", PeertubeUrl.channelTabSuffix("playlists"))
        assertNull(PeertubeUrl.channelTabSuffix("unsupported"))
    }

    @Test
    fun encodesSearchTermsForQueryParameters() {
        assertEquals("art+%26+caf%C3%A9", PeertubeUrl.encodeSearchQuery("art & café"))
        assertEquals("a%7E%21*%27%28%29+%2B%26", PeertubeUrl.encodeSearchQuery("a~!*'() +&"))
        assertEquals("", PeertubeUrl.encodeSearchQuery(""))
    }
}
