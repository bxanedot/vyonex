// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayerRequestBudgetContractTest {
    @Test
    fun consumesExactlyTheAvailableRequestBudget() {
        val budget = PlayerRequestBudget(2)

        assertEquals(2, budget.remaining)
        budget.consume()
        budget.consume()
        assertEquals(0, budget.remaining)
        assertFailsWith<IllegalStateException> { budget.consume() }
    }

    @Test
    fun rejectsAnEmptyRequestBudget() {
        assertFailsWith<IllegalArgumentException> { PlayerRequestBudget(0) }
    }
}
