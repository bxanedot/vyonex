// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaCCCLiveRoomResolverTest {
    @Test
    fun resolvesRoomMetadataThumbnailsAndPlayableFormats() {
        val conferences = MediaCCCLivePayloadDecoder.decode(
            """[{"conference":"Chaos","slug":"chaos-2026","description":"Event details",
                "groups":[{"group":"Hall A","rooms":[{"slug":"a","display":"Room A",
                  "thumb":"https://cdn.example/thumb.jpg","poster":"https://cdn.example/poster.jpg",
                  "streams":[
                    {"type":"video","videoSize":[1920,1080],"urls":{
                      "dash":{"url":"https://cdn.example/room.mpd"},
                      "hls":{"url":"https://cdn.example/room.m3u8","tech":"hls"},
                      "mp4":{"url":"https://cdn.example/room.mp4","tech":"h264"}}},
                    {"type":"audio","urls":{"mp3":{"url":"https://cdn.example/room.mp3","tech":"mp3"}}}]}]}]}]""",
        )

        val room = MediaCCCLiveRoomResolver.findRoom(conferences, "chaos-2026/a")

        assertEquals("Room A", room?.display)
        assertEquals("Chaos", room?.conferenceName)
        assertEquals("chaos-2026", room?.conferenceSlug)
        assertEquals("Event details", room?.conferenceDescription)
        assertEquals("Hall A", room?.group)
        assertEquals(
            listOf(
                MediaCCCLiveThumbnail("https://cdn.example/thumb.jpg", MediaCCCLiveThumbnail.Quality.MEDIUM),
                MediaCCCLiveThumbnail("https://cdn.example/poster.jpg", MediaCCCLiveThumbnail.Quality.HIGH),
            ),
            room?.thumbnails,
        )
        assertEquals("https://cdn.example/room.mpd", room?.manifest("dash"))
        assertEquals("https://cdn.example/room.m3u8", room?.manifest("hls"))
        assertEquals(3, room?.streams?.first()?.formats?.size)
        assertEquals(1920, room?.streams?.first()?.width)
        assertEquals(1080, room?.streams?.first()?.height)
        assertEquals("audio", room?.streams?.last()?.type)
        assertEquals("https://cdn.example/room.mp3", room?.streams?.last()?.formats?.single()?.url)
    }

    @Test
    fun ignoresConferencesAndRoomsWithoutMatchingSlugs() {
        val conferences = MediaCCCLivePayloadDecoder.decode(
            """[{"groups":[{"rooms":[{"slug":"a"}]}]},
                {"slug":"chaos-2026","groups":[{"rooms":[{"display":"Room without slug"}]}]}]""",
        )

        assertNull(MediaCCCLiveRoomResolver.findRoom(conferences, "chaos-2026/a"))
    }
}
