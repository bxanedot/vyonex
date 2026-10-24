// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BandcampFeaturedPageParserTest {
    @Test
    fun parsesInitialFeaturedItemsAndBuildsCursorFromTheLastStory() {
        val page = BandcampFeaturedPageParser.parseInitial(
            """{"feed_content":{"stories":{"featured":[
                {"album_title":"Record","band_name":"Artist","item_url":"http://artist.bandcamp.com/album/record",
                 "art_id":"bad","item_art_id":12,"num_streamable_tracks":3},
                {"story_date":101,"ntid":201,"story_type":"video"}]}}}""",
        )

        assertEquals(
            listOf(BandcampFeaturedItem(
                "Record", "Artist", "http://artist.bandcamp.com/album/record", 3, 12,
            )),
            page.items,
        )
        assertEquals("featured:101:video:201", page.nextStoryGroup)
    }

    @Test
    fun parsesOlderPagesAndOmitsCursorsWithoutNumericFields() {
        val page = BandcampFeaturedPageParser.parseOlder(
            """{"stories":{"featured":[{"album_title":"Older","story_date":"101",
                "ntid":201,"story_type":"album"}]}}""",
        )

        assertEquals("Older", page.items.single().title)
        assertEquals(-1, page.items.single().streamCount)
        assertEquals(0, page.items.single().artworkId)
        assertNull(page.nextStoryGroup)
    }

    @Test
    fun distinguishesMalformedResponsesFromMissingStoryArrays() {
        assertFailsWith<BandcampFeaturedPageParseException> {
            BandcampFeaturedPageParser.parseInitial("not-json")
        }
        assertFailsWith<BandcampFeaturedStoriesMissingException> {
            BandcampFeaturedPageParser.parseOlder("{}")
        }
    }
}
