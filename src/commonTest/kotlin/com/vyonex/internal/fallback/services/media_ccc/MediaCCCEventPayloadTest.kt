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

    @Test
    fun decodesConferenceAndEventSearchListings() {
        val conference = MediaCCCListingPayloadDecoder.decodeConference(
            """{"title":"FOSDEM","logo_url":"https://cdn.example/logo.jpg",
                "events":[{"title":"Talk","guid":"talk-1",
                  "conference_title":"FOSDEM","conference_url":"https://media.ccc.de/c/fosdem",
                  "release_date":"2026-02-01T12:00:00Z","length":1200,
                  "view_count":34,"thumb_url":"https://cdn.example/thumb.jpg"}],
                "future_field":true}""",
        )
        val event = conference.events.single()

        assertEquals("FOSDEM", conference.title)
        assertEquals("https://cdn.example/logo.jpg", conference.logoUrl)
        assertEquals("talk-1", event.guid)
        assertEquals("FOSDEM", event.conferenceTitle)
        assertEquals(1200L, event.length)
        assertEquals("https://cdn.example/thumb.jpg", event.thumbUrl)

        val search = MediaCCCListingPayloadDecoder.decodeEventSearch(
            """{"events":[{"title":"Found talk","guid":"found-1",
                "release_date":"2026-02-01T12:00:00Z"}],"future_field":true}""",
        )
        assertEquals("Found talk", search.events.single().title)
        assertEquals(emptyList(), MediaCCCListingPayloadDecoder.decodeConference("{}").events)

        val recent = MediaCCCListingPayloadDecoder.decodeRecentEvents(
            """{"events":[{"title":"Recent talk","frontend_link":"https://media.ccc.de/v/recent",
                "date":"2026-02-01T12:00:00.000UTC","duration":600,"view_count":17,
                "poster_url":"https://cdn.example/poster.jpg"}]}""",
        ).events.single()
        assertEquals("https://media.ccc.de/v/recent", recent.frontendLink)
        assertEquals("2026-02-01T12:00:00.000UTC", recent.date)
        assertEquals(600L, recent.duration)
        assertEquals(17L, recent.viewCount)
        assertEquals("https://cdn.example/poster.jpg", recent.posterUrl)
    }

    @Test
    fun decodesConferenceDirectoryWithOptionalLogoAndUnknownFields() {
        val listing = MediaCCCListingPayloadDecoder.decodeConferenceList(
            """{"Conferences":"Conferences","conferences":[
                {"title":"FOSDEM","url":"https://media.ccc.de/c/fosdem",
                 "logo_url":"https://cdn.example/fosdem.jpg","future":true},
                {"title":"No logo","url":"https://media.ccc.de/c/no-logo"}],
                "future_field":true}""",
        )

        assertEquals("Conferences", listing.title)
        assertEquals("FOSDEM", listing.conferences[0].title)
        assertEquals("https://media.ccc.de/c/fosdem", listing.conferences[0].url)
        assertEquals("https://cdn.example/fosdem.jpg", listing.conferences[0].logoUrl)
        assertNull(listing.conferences[1].logoUrl)
        assertEquals(emptyList(), MediaCCCListingPayloadDecoder.decodeConferenceList("{}").conferences)
    }

    @Test
    fun malformedListingPayloadsFailAtTheDecodeBoundary() {
        assertFailsWith<MediaCCCListingParseException> {
            MediaCCCListingPayloadDecoder.decodeConference("not-json")
        }
        assertFailsWith<MediaCCCListingParseException> {
            MediaCCCListingPayloadDecoder.decodeEventSearch("""{"events":[{"length":"invalid"}]}""")
        }
        assertFailsWith<MediaCCCListingParseException> {
            MediaCCCListingPayloadDecoder.decodeConferenceList("""{"conferences":[{"title":4}]}""")
        }
    }
}
