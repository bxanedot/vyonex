// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YoutubePlayabilityStatusParserTest {
    @Test
    fun acceptsMissingAndSuccessfulStatus() {
        assertNull(YoutubePlayabilityStatusParser.classify(null, "Unavailable"))
        assertNull(YoutubePlayabilityStatusParser.classify("OK", null))
        assertNull(YoutubePlayabilityStatusParser.classify("oK", null))
    }

    @Test
    fun mapsFailureStatusAndReasonToTheFallbackCategory() {
        assertEquals(
            YoutubePlayabilityFailure.AGE_RESTRICTED,
            YoutubePlayabilityStatusParser.classify("LOGIN_REQUIRED", "Inappropriate for some users"),
        )
        assertEquals(
            YoutubePlayabilityFailure.PRIVATE,
            YoutubePlayabilityStatusParser.classify("login_required", "This video is private"),
        )
        assertEquals(
            YoutubePlayabilityFailure.BOT_CONFIRMATION,
            YoutubePlayabilityStatusParser.classify("login_required", "Confirm you are not a bot"),
        )
        assertEquals(
            YoutubePlayabilityFailure.MUSIC_PREMIUM,
            YoutubePlayabilityStatusParser.classify("unplayable", "Music Premium required"),
        )
        assertEquals(
            YoutubePlayabilityFailure.PAID,
            YoutubePlayabilityStatusParser.classify("error", "Payment required"),
        )
        assertEquals(
            YoutubePlayabilityFailure.MEMBERS_ONLY,
            YoutubePlayabilityStatusParser.classify("unplayable", "Members only"),
        )
        assertEquals(
            YoutubePlayabilityFailure.COUNTRY_RESTRICTED,
            YoutubePlayabilityStatusParser.classify("unplayable", "Unavailable in this country"),
        )
        assertEquals(
            YoutubePlayabilityFailure.ACCOUNT_TERMINATED,
            YoutubePlayabilityStatusParser.classify("error", "Account terminated"),
        )
        assertEquals(
            YoutubePlayabilityFailure.UNAVAILABLE,
            YoutubePlayabilityStatusParser.classify("unplayable", "Unknown failure"),
        )
    }
}
