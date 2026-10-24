// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BandcampReviewPageParserTest {
    @Test
    fun parsesEmbeddedReviewDataAndKeepsCursorWhenLaterReviewsOmitIt() {
        val page = BandcampReviewPageParser.parseEmbedded(
            """{"reviews":[{"why":"Nice release","name":"Fan","image_id":45,
                "token":"cursor-one"},null,{"why":"Second review"}],
                "more_reviews_available":true}""",
        )

        assertEquals(2, page.reviews.size)
        assertEquals("Nice release", page.reviews[0].text)
        assertEquals("Fan", page.reviews[0].uploaderName)
        assertEquals(45, page.reviews[0].avatarId)
        assertEquals("Second review", page.reviews[1].text)
        assertEquals("cursor-one", page.nextToken)
        assertTrue(page.moreAvailable)
    }

    @Test
    fun parsesApiResponsesAndDefaultsMissingOrWrongTypeFields() {
        val page = BandcampReviewPageParser.parseResponse(
            """{"results":[{"why":2,"name":false,"image_id":"45","token":3}],
                "more_available":"true"}""",
        )

        assertEquals("", page.reviews.single().text)
        assertEquals("", page.reviews.single().uploaderName)
        assertEquals(0, page.reviews.single().avatarId)
        assertEquals(null, page.reviews.single().token)
        assertFalse(page.moreAvailable)
        assertEquals(0, BandcampReviewPageParser.parseEmbedded("{}").reviews.size)
        assertEquals(null, page.nextToken)
        assertEquals(123L, BandcampReviewPageParser.parseTrackId("""{"item_id":123}"""))
    }

    @Test
    fun rejectsMalformedOrNonObjectResponses() {
        assertFailsWith<BandcampReviewPageParseException> {
            BandcampReviewPageParser.parseEmbedded("not-json")
        }
        assertFailsWith<BandcampReviewPageParseException> {
            BandcampReviewPageParser.parseResponse("[]")
        }
    }
}
