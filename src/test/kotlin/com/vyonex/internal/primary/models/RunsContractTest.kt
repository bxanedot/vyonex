// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class RunsContractTest {
    @Test
    fun splitsRunGroupsAtTheBulletSeparatorAndKeepsEmptyGroups() {
        assertEquals(
            listOf(listOf(Run("Views")), listOf(Run("Today"))),
            listOf(Run("Views"), Run(" • "), Run("Today")).splitBySeparator(),
        )
        assertEquals(listOf(emptyList<Run>()), emptyList<Run>().splitBySeparator())
    }

    @Test
    fun cleansHeaderRowsUnlessFirstRowIsAlreadyContent() {
        val headerAndContent = listOf(listOf(Run("Header")), listOf(Run("Value")))
        assertEquals(listOf(listOf(Run("Value"))), headerAndContent.clean())

        val joinedLabels = listOf(listOf(Run("Views, likes")))
        assertSame(joinedLabels, joinedLabels.clean())

        val linked = listOf(listOf(Run("Channel", navigationEndpoint = NavigationEndpoint())))
        assertSame(linked, linked.clean())
    }

    @Test
    fun selectsEveryOtherRunStartingAtTheFirst() {
        assertEquals(
            listOf(Run("one"), Run("three")),
            listOf(Run("one"), Run("two"), Run("three")).oddElements(),
        )
    }
}
