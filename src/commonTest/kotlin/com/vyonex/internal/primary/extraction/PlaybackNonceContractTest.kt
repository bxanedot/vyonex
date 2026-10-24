// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaybackNonceContractTest {
    @Test
    fun appendsAndReplacesNonceOnlyOnApprovedMediaUrls() {
        val nonce = "Abcdef0123456789"
        val original = "https://r1.googlevideo.com/videoplayback?id=1#frag"

        assertEquals(
            "https://r1.googlevideo.com/videoplayback?id=1&cpn=$nonce#frag",
            appendClientPlaybackNonce(original, nonce),
        )
        val alreadyTagged = "https://r1.googlevideo.com/videoplayback?id=1&cpn=existing#frag"
        assertEquals(alreadyTagged, appendClientPlaybackNonce(alreadyTagged, nonce))
        assertEquals(
            "https://r1.googlevideo.com/videoplayback?id=1&cpn=$nonce#frag",
            replaceClientPlaybackNonce(
                "https://r1.googlevideo.com/videoplayback?id=1&cpn=old#frag",
                nonce,
            ),
        )
        val unapproved = "https://example.com/videoplayback"
        assertEquals(unapproved, appendClientPlaybackNonce(unapproved, nonce))
        assertEquals(original, appendClientPlaybackNonce(original, "bad"))
    }

    @Test
    fun generatedNonceUsesTheExpectedAlphabetAndLength() {
        assertTrue(generateClientPlaybackNonce().matches(Regex("[A-Za-z0-9_-]{16}")))
    }
}
