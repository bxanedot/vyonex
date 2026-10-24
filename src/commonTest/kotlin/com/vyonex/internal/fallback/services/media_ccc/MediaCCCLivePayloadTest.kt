// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MediaCCCLivePayloadTest {
    @Test
    fun decodesRoomsTalksAndMultipleDeliveryFormats() {
        val conference = MediaCCCLivePayloadDecoder.decode(
            """[{"conference":"Chaos","slug":"chaos-2026","description":"Annual event",
                "groups":[{"group":"Hall A","rooms":[{"slug":"a","display":"Room A",
                  "link":"https://streaming.media.ccc.de/chaos-2026/a",
                  "talks":{"current":{"title":"Live now"}},
                  "thumb":"https://cdn.example/thumb.jpg",
                  "streams":[{"type":"video","videoSize":[1920,1080],"urls":{
                    "dash":{"url":"https://cdn.example/room.mpd","tech":"dash"},
                    "hls":{"url":"https://cdn.example/room.m3u8","tech":"hls"},
                    "invalid":null}}]}]}],"future_field":true}]""",
        ).single()
        val room = conference.groups?.single()?.rooms?.single()
        val stream = room?.streams?.single()

        assertEquals("chaos-2026", conference.slug)
        assertEquals("Live now", room?.talks?.current?.title)
        assertEquals(listOf(1920, 1080), stream?.videoSize)
        assertEquals(
            listOf(
                MediaCCCLiveSourcePayload("dash", "dash", "https://cdn.example/room.mpd"),
                MediaCCCLiveSourcePayload("hls", "hls", "https://cdn.example/room.m3u8"),
            ),
            stream?.sources(),
        )
    }

    @Test
    fun decodesAudioStreamsWithoutVideoDimensionsAndOptionalRoomFields() {
        val room = MediaCCCLivePayloadDecoder.decode(
            """[{"slug":"past","groups":[{"rooms":[{"slug":"audio",
                "streams":[{"type":"audio","videoSize":null,
                  "urls":{"mp3":{"url":"https://cdn.example/audio.mp3"}}}]}]}]}]""",
        ).single().groups?.single()?.rooms?.single()

        assertNull(room?.display)
        assertNull(room?.streams?.single()?.videoSize)
        assertEquals(
            "https://cdn.example/audio.mp3",
            room?.streams?.single()?.sources()?.single()?.url,
        )
    }

    @Test
    fun malformedResponsesFailAtTheDecodeBoundary() {
        assertFailsWith<MediaCCCLivePayloadParseException> {
            MediaCCCLivePayloadDecoder.decode("not-json")
        }
    }
}
