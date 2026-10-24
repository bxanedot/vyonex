// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YoutubeContinuationTokenParserTest {
    @Test
    fun readsSearchContinuationTokens() {
        assertEquals(
            "next-token",
            YoutubeContinuationTokenParser.parse(
                """{"continuationEndpoint":{"continuationCommand":{"token":"next-token"}}}""",
            ),
        )
        assertEquals(
            " ",
            YoutubeContinuationTokenParser.parse(
                """{"continuationEndpoint":{"continuationCommand":{"token":" "}}}""",
            ),
        )
    }

    @Test
    fun ignoresMissingEmptyAndWronglyTypedTokens() {
        listOf(
            "{}",
            """{"continuationEndpoint":{}}""",
            """{"continuationEndpoint":{"continuationCommand":{"token":""}}}""",
            """{"continuationEndpoint":{"continuationCommand":{"token":3}}}""",
            "not-json",
            "[]",
        ).forEach { source -> assertNull(YoutubeContinuationTokenParser.parse(source)) }
    }
}
