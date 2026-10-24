// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeertubeCaptionResponseParserTest {
    @Test
    fun prefersCurrentCaptionUrlAndSupportsOlderUrlFields() {
        assertEquals(
            listOf(
                PeertubeCaption("en", "/captions/current.vtt", true),
                PeertubeCaption("fr", "/captions/legacy.srt", false),
                PeertubeCaption("de", "/captions/hls.m3u8", false),
            ),
            PeertubeCaptionResponseParser.parse(
                """{"data":[
                    {"language":{"id":"en"},"fileUrl":"/captions/current.vtt",
                     "captionPath":"/captions/old.vtt","m3u8Url":"/captions/older.m3u8",
                     "automaticallyGenerated":true},
                    {"language":{"id":"fr"},"captionPath":"/captions/legacy.srt"},
                    {"language":{"id":"de"},"m3u8Url":"/captions/hls.m3u8"}
                ]}""",
            ),
        )
    }

    @Test
    fun skipsInvalidCaptionsAndTreatsMissingDataAsEmpty() {
        assertEquals(
            listOf(PeertubeCaption("en", "/captions/en.vtt", false)),
            PeertubeCaptionResponseParser.parse(
                """{"data":[null,4,{"language":{"id":" "},"fileUrl":"/bad.vtt"},
                    {"language":{"id":"en"},"fileUrl":"/captions/en.vtt",
                     "automaticallyGenerated":"true"},{"language":{"id":"fr"}}]}""",
            ),
        )
        assertEquals(emptyList(), PeertubeCaptionResponseParser.parse("{}"))
    }

    @Test
    fun rejectsNonObjectResponses() {
        assertFailsWith<IllegalArgumentException> {
            PeertubeCaptionResponseParser.parse("[]")
        }
    }
}
