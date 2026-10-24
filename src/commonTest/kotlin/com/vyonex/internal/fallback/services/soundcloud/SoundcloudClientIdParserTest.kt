// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SoundcloudClientIdParserTest {
    @Test
    fun extractsTheFirstQuotedClientIdWithEitherSeparator() {
        assertEquals("colon-key", SoundcloudClientIdParser.parse("client_id : 'colon-key'"))
        assertEquals("equals-key", SoundcloudClientIdParser.parse("client_id=\"equals-key\""))
    }

    @Test
    fun returnsNullWhenNoNonEmptyQuotedClientIdIsPresent() {
        assertNull(SoundcloudClientIdParser.parse("CLIENT_ID='ignored' client_id=''"))
    }
}
