// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MediaCCCEventPayloadTest {
    @Test
    fun decodesEventMetadataAndRecordingVariants() {
        val event = MediaCCCEventPayload.decode(
            """{"title":"Talk","release_date":"2026-02-01T12:00:00Z",
                "description":"Systems","length":1200,"view_count":34,
                "conference_url":"https://media.ccc.de/c/fosdem",
                "frontend_link":"https://media.ccc.de/v/talk",
                "original_language":"deu","tags":["linux",42,null],
                "thumb_url":"https://cdn.example/thumb.jpg",
                "poster_url":"https://cdn.example/poster.jpg",
                "recordings":[
                  {"filename":"audio-de","recording_url":"https://cdn.example/audio.mp3",
                   "mime_type":"audio/mpeg","language":"de"},
                  {"filename":"video-720","recording_url":"https://cdn.example/video.mp4",
                   "mime_type":"video/mp4","height":720}],
                "future_field":{"ignored":true}}""",
        )

        assertEquals("Talk", event.title)
        assertEquals("2026-02-01T12:00:00Z", event.releaseDate)
        assertEquals(1200L, event.length)
        assertEquals(listOf("linux"), event.stringTags())
        assertEquals("fosdem", event.conferenceUrl?.substringAfterLast('/'))
        assertEquals("audio/mpeg", event.recordings?.first()?.mimeType)
        assertEquals(720, event.recordings?.last()?.height)
        assertEquals("https://cdn.example/poster.jpg", event.posterUrl)
    }

    @Test
    fun missingOptionalFieldsUseEmptyCollectionsAndNullMetadata() {
        val event = MediaCCCEventPayload.decode("""{"title":"Talk","recordings":null}""")

        assertNull(event.releaseDate)
        assertEquals(emptyList(), event.stringTags())
        assertEquals(null, event.recordings)
    }

    @Test
    fun malformedPayloadsFailAtTheDecodeBoundary() {
        assertFailsWith<MediaCCCEventParseException> {
            MediaCCCEventPayload.decode("not-json")
        }
    }
}
