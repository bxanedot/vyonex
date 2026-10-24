// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.url

import io.ktor.http.encodeURLParameter

internal object FormUrlEncoder {
    // Match Java's historical form encoding for '~' and '*'.
    fun encode(value: String): String =
        value.encodeURLParameter(spaceToPlus = true).replace("~", "%7E").replace("%2A", "*")
}
