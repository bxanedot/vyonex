// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeertubeStoryboardResponseParserTest {
    @Test
    fun mapsValidGeometryAndPrefersFileUrl() {
        assertEquals(
            listOf(PeertubeStoryboard("/sprites/file.jpg", 160, 90, 4, 10, 2, 2)),
            PeertubeStoryboardResponseParser.parse(
                """{"storyboards":[null,{"fileUrl":"/sprites/file.jpg","storyboardPath":"/fallback.jpg","spriteWidth":160,"spriteHeight":90,"totalWidth":320,"totalHeight":180,"spriteDuration":10}]}""",
            ),
        )
    }

    @Test
    fun skipsInvalidGeometryAndOverflow() {
        assertEquals(
            listOf(PeertubeStoryboard("/sprites/fallback.jpg", 1, 1, 1, 1, 1, 1)),
            PeertubeStoryboardResponseParser.parse(
                """{"storyboards":[{"spriteWidth":1,"spriteHeight":1,"totalWidth":1,"totalHeight":1,"spriteDuration":1,"storyboardPath":"/sprites/fallback.jpg"},{"fileUrl":"/bad.jpg","spriteWidth":0,"spriteHeight":1,"totalWidth":1,"totalHeight":1,"spriteDuration":1},{"fileUrl":"/too-many.jpg","spriteWidth":1,"spriteHeight":1,"totalWidth":2147483647,"totalHeight":2147483647,"spriteDuration":1}]}""",
            ),
        )
    }

    @Test
    fun handlesMissingStoryboardsAndRejectsNonObjectResponses() {
        assertEquals(emptyList(), PeertubeStoryboardResponseParser.parse("{}"))
        assertFailsWith<IllegalArgumentException> {
            PeertubeStoryboardResponseParser.parse("[]")
        }
    }
}
