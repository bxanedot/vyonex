// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YoutubeWatchPlayerResponseParserTest {
    @Test
    fun parsesAssignedPlayerResponseWithoutTreatingStringBracesAsJsonStructure() {
        val response =
            YoutubeWatchPlayerResponseParser.parse(
                """
                <script>
                window["ytInitialPlayerResponse"] = {
                  "playabilityStatus":{"status":"OK"},
                  "videoDetails":{"videoId":"abcdefghijk","title":"part }; two"},
                  "streamingData":{"adaptiveFormats":[
                    {"itag":140,"mimeType":"audio/mp4","bitrate":128000,"url":"https://media.example/audio"}
                  ]}
                };
                </script>
                """.trimIndent(),
            )

        assertEquals("OK", response?.playabilityStatus?.status)
        assertEquals("part }; two", response?.videoDetails?.title)
        assertEquals(
            "https://media.example/audio",
            response?.streamingData?.adaptiveFormats?.single()?.url,
        )
    }

    @Test
    fun returnsNullWhenTheWatchPageHasNoAssignedPlayerResponse() {
        assertNull(
            YoutubeWatchPlayerResponseParser.parse(
                """<script>window.config = {"ytInitialPlayerResponse":{"playabilityStatus":{"status":"OK"}}};</script>""",
            ),
        )
    }
}
