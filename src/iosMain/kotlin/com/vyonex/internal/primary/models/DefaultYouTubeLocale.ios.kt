// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.models

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

internal actual fun defaultYouTubeLocale(): YouTubeLocale {
    val languageTag = ((NSLocale.preferredLanguages.firstOrNull() as? String).orEmpty().ifBlank { "en" }).replace('_', '-')
    val region = languageTag.split('-').drop(1).firstOrNull { it.length == 2 }.orEmpty()
    return YouTubeLocale(
        gl = region.uppercase(),
        hl = languageTag,
    )
}
