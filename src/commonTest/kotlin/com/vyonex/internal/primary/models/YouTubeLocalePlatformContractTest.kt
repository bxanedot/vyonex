// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.models

import kotlin.test.Test
import kotlin.test.assertTrue

class YouTubeLocalePlatformContractTest {
    @Test
    fun defaultPlatformLocaleProvidesAnAcceptLanguageHeader() {
        assertTrue(defaultYouTubeLocale().acceptLanguageHeader().isNotBlank())
    }
}
