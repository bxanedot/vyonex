// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import com.vyonex.internal.primary.extraction.StreamResolveException.Reason
import com.vyonex.internal.primary.extraction.strategy.ClientFailureKind
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayabilityFailureContractTest {
    @Test
    fun classifiesStatusAndReasonAndPrioritizesAgeRestrictions() {
        val age = PlayabilityFailure(null, "Please confirm your age")
        val unavailable = PlayabilityFailure("VIDEO_UNAVAILABLE", "Video unavailable")
        val login = PlayabilityFailure("LOGIN_REQUIRED", null)

        assertEquals(ClientFailureKind.PLAYABILITY, age.category)
        assertEquals(ClientFailureKind.TOKEN, login.category)
        assertEquals(Reason.AGE_RESTRICTED, classifyPlayabilityFailures(listOf(unavailable, age)))
        assertEquals(Reason.UNAVAILABLE, classifyPlayabilityFailures(listOf(unavailable)))
        assertEquals(null, classifyPlayabilityFailures(listOf(login)))
    }
}
