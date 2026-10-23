// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class YouTubeMediaUrlPolicyContractTest {
    @Test
    fun acceptsOnlySecureDeliveryHostsAndTheDirectPlaybackPath() {
        assertTrue(isAllowedMediaUrl("https://rr1.googlevideo.com/videoplayback?itag=18"))
        assertTrue(isAllowedMediaUrl("https://music.youtube.com/videoplayback"))
        assertFalse(isAllowedMediaUrl("http://rr1.googlevideo.com/videoplayback"))
        assertFalse(isAllowedMediaUrl("https://rr1.googlevideo.com:444/videoplayback"))
        assertFalse(isAllowedMediaUrl("https://googlevideo.com.attacker.test/videoplayback"))
        assertFalse(isAllowedMediaUrl("https://attacker@rr1.googlevideo.com/videoplayback"))
        assertFalse(isAllowedMediaUrl("https://rr1.googlevideo.com/videoplayback/extra"))
        assertFalse(isAllowedMediaUrl("https://rr1.googlevideo.com/manifest/dash/segment"))
    }

    @Test
    fun acceptsOnlySecureDeliveryHostsAndManifestPathsForHls() {
        assertTrue(isAllowedHlsUrl("https://rr1.googlevideo.com/manifest/dash/playlist"))
        assertTrue(isAllowedHlsUrl("https://www.youtube.com/api/manifest/hls_variant/playlist"))
        assertFalse(isAllowedHlsUrl("http://rr1.googlevideo.com/manifest/dash/playlist"))
        assertFalse(isAllowedHlsUrl("https://googlevideo.com.attacker.test/manifest/dash/playlist"))
        assertFalse(isAllowedHlsUrl("https://attacker@rr1.googlevideo.com/manifest/dash/playlist"))
        assertFalse(isAllowedHlsUrl("https://rr1.googlevideo.com/manifestation/playlist"))
        assertFalse(isAllowedHlsUrl("https://rr1.googlevideo.com/api/manifest"))
        assertFalse(isAllowedHlsUrl("https://rr1.googlevideo.com/videoplayback"))
    }
}
