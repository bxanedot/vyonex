// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeertubeAccountChannelsParserTest {
    @Test
    fun sumsFollowerCountsAndIgnoresNonObjectEntries() {
        assertEquals(
            5L,
            PeertubeAccountChannelsParser.followerCount(
                """{"data":[{"followersCount":2},{"followersCount":3},{},"ignored"]}""",
            ),
        )
    }

    @Test
    fun rejectsMalformedResponsesAndMissingChannelArrays() {
        assertFailsWith<PeertubeAccountChannelsParseException> {
            PeertubeAccountChannelsParser.followerCount("not-json")
        }
        assertFailsWith<PeertubeAccountChannelsParseException> {
            PeertubeAccountChannelsParser.followerCount("""{"data":{}}""")
        }
    }
}
