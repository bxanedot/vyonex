// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BandcampRadioEpisodeParserTest {
    @Test
    fun parsesEpisodeMetadataAudioAndNonEmptyTrackSegments() {
        val episode = BandcampRadioEpisodeParser.parse(
            """{"subtitle":"Weekly episode","image_caption":"<a>Host</a>",
                "published_date":"2024-06-15","show_image_id":12,"desc":"Episode description",
                "audio_duration":3600,"audio_stream":{"mp3-128":"https://audio.example/episode.mp3",
                "opus-lo":"https://audio.example/episode.opus"},"tracks":[
                {"title":"Song","timecode":30,"track_art_id":34,"artist":"Artist"},
                {"title":" "},null]}""",
        )

        assertEquals("Weekly episode", episode.subtitle)
        assertEquals("<a>Host</a>", episode.imageCaption)
        assertEquals("2024-06-15", episode.publishedDate)
        assertEquals(12, episode.imageId)
        assertEquals("Episode description", episode.description)
        assertEquals(3600, episode.duration)
        assertEquals("https://audio.example/episode.mp3", episode.mp3Url)
        assertEquals("https://audio.example/episode.opus", episode.opusUrl)
        assertEquals(listOf(BandcampRadioEpisodeSegment("Song", 30, 34, "Artist")), episode.segments)
    }

    @Test
    fun appliesEmptyFieldDefaultsAndRejectsMalformedOrNonObjectData() {
        assertEquals(
            BandcampRadioEpisodePayload(null, null, null, 0, null, 0, null, null, emptyList()),
            BandcampRadioEpisodeParser.parse("{}"),
        )
        assertFailsWith<BandcampRadioEpisodeParseException> {
            BandcampRadioEpisodeParser.parse("not-json")
        }
        assertFailsWith<BandcampRadioEpisodeParseException> {
            BandcampRadioEpisodeParser.parse("[]")
        }
    }
}
