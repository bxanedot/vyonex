// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler

internal object SoundcloudTrackUrlParser {
    private val pageUrl = Regex(
        """^https?://(?:(?:www|m)\.)?soundcloud\.com/[a-z0-9_-]+/""" +
            """(?!tracks/?$|albums/?$|sets/?$|reposts/?$|followers/?$|following/?$)[a-z0-9_-]+/?(?:[?#].*)?$|""" +
            """^https?://on\.soundcloud\.com/[a-z0-9]+/?$""",
        RegexOption.IGNORE_CASE,
    )
    private val apiUrl = Regex(
        """^https?://api-v2\.soundcloud\.com/(?:tracks|albums|sets)/([^/?#]+)(?:[/?#].*)?$""",
        RegexOption.IGNORE_CASE,
    )

    fun acceptsPage(value: String): Boolean = pageUrl.matches(value)

    fun apiTrackId(value: String): String? = apiUrl.matchEntire(value)?.groupValues?.get(1)
}
