// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SoundcloudTrackUrlParserTest {
    @Test
    fun recognizesTrackPagesAndShortLinks() {
        listOf(
            "https://soundcloud.com/artist/song",
            "http://www.soundcloud.com/artist/song?token=abc",
            "https://m.soundcloud.com/artist/song",
            "https://on.soundcloud.com/a12BC",
        ).forEach { assertTrue(SoundcloudTrackUrlParser.acceptsPage(it), it) }

        listOf(
            "https://soundcloud.com/artist/tracks",
            "https://soundcloud.com/artist/sets",
            "https://soundcloud.com/artist/followers",
            "https://soundcloud.com/artist",
            "https://example.com/artist/song",
        ).forEach { assertFalse(SoundcloudTrackUrlParser.acceptsPage(it), it) }
    }

    @Test
    fun extractsApiTrackIdsFromSupportedResourceUrls() {
        assertEquals("42", SoundcloudTrackUrlParser.apiTrackId("https://api-v2.soundcloud.com/tracks/42"))
        assertEquals("mix", SoundcloudTrackUrlParser.apiTrackId("https://api-v2.soundcloud.com/sets/mix?client_id=x"))
        assertEquals("album", SoundcloudTrackUrlParser.apiTrackId("https://api-v2.soundcloud.com/albums/album/"))
        assertNull(SoundcloudTrackUrlParser.apiTrackId("https://api-v2.soundcloud.com/users/42"))
    }
}
