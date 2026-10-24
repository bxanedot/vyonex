// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeertubeImageParserTest {
    @Test
    fun keepsThumbnailOrderAndSkipsBlankPaths() {
        assertEquals(
            listOf(
                PeertubeImageReference("/video.jpg", size = PeertubeImageSize.LOW),
                PeertubeImageReference("/preview.jpg", size = PeertubeImageSize.MEDIUM),
            ),
            PeertubeImageParser.thumbnails("/video.jpg", "/preview.jpg"),
        )
        assertEquals(emptyList(), PeertubeImageParser.thumbnails(" ", null))
    }

    @Test
    fun parsesProfileImageListsAndUsesSingleImageFallbackOnlyWhenListIsEmpty() {
        assertEquals(
            listOf(PeertubeImageReference("/avatar.jpg", 96)),
            PeertubeImageParser.profileImages(
                """{"avatars":[null,{"path":""},{"path":"/avatar.jpg","width":96}],
                    "avatar":{"path":"/fallback.jpg"}}""",
                "avatars",
                "avatar",
            ),
        )
        assertEquals(
            listOf(PeertubeImageReference("/fallback.jpg", 48)),
            PeertubeImageParser.profileImages(
                """{"avatars":[],"avatar":{"path":"/fallback.jpg","width":48}}""",
                "avatars",
                "avatar",
            ),
        )
        assertEquals(
            emptyList(),
            PeertubeImageParser.profileImages(
                """{"avatars":[null],"avatar":{"path":"/fallback.jpg"}}""",
                "avatars",
                "avatar",
            ),
        )
    }

    @Test
    fun rejectsNonObjectImagePayloads() {
        assertFailsWith<IllegalArgumentException> {
            PeertubeImageParser.profileImages("[]", "avatars", "avatar")
        }
    }
}
