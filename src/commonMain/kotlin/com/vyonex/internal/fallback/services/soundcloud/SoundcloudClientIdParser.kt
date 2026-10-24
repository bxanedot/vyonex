// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

internal object SoundcloudClientIdParser {
    private val clientId = Regex("""client_id\s*[:=]\s*["']([^"']+)["']""")

    fun parse(source: String): String? = clientId.find(source)?.groupValues?.get(1)
}
