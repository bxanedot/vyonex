// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler

internal object BandcampRadioUrlParser {
    private val showQuery = Regex("show=[0-9]+")

    fun accepts(value: String?): Boolean {
        val url = BandcampUrlParser.parse(value) ?: return false
        return url.host.equals("bandcamp.com", ignoreCase = true) &&
            (url.path.isEmpty() || url.path == "/") &&
            url.rawQuery?.matches(showQuery) == true
    }
}
