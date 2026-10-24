// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.youtube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class YoutubeFormatMetadataParserTest {
    @Test
    fun `maps audio MIME and average bitrate`() {
        assertEquals(
            YoutubeFormatMetadata(140, "audio/mp4", 128000, "", -1),
            YoutubeFormatMetadataParser.parse(
                """{
                    "itag":140,
                    "mimeType":"audio/mp4; codecs=\"mp4a.40.2\"",
                    "averageBitrate":128000
                }""",
                audio = true,
            ),
        )
    }

    @Test
    fun `uses quality label or derives resolution from height`() {
        assertEquals(
            YoutubeFormatMetadata(18, "video/mp4", -1, "720p60", 60),
            YoutubeFormatMetadataParser.parse(
                """{
                    "itag":18,
                    "mimeType":"video/mp4; codecs=\"avc1\"",
                    "qualityLabel":"720p60",
                    "fps":60
                }""",
                audio = false,
            ),
        )
        assertEquals(
            "480p",
            YoutubeFormatMetadataParser.parse(
                """{"itag":135,"mimeType":"video/mp4","height":480}""",
                audio = false,
            ).resolution,
        )
    }

    @Test
    fun `preserves existing defaults for absent or wrongly typed fields`() {
        assertEquals(
            YoutubeFormatMetadata(0, "", -1, "", -1),
            YoutubeFormatMetadataParser.parse(
                """{"itag":"18","mimeType":4,"height":"360","fps":null}""",
                audio = false,
            ),
        )
    }

    @Test
    fun `rejects non-object format JSON`() {
        assertFailsWith<IllegalArgumentException> {
            YoutubeFormatMetadataParser.parse("[]", audio = false)
        }
    }
}
