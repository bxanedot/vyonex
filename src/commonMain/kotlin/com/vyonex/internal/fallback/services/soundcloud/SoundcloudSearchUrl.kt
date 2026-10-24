// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import com.vyonex.internal.url.FormUrlEncoder

internal object SoundcloudSearchUrl {
    fun build(query: String, filters: List<String>, clientId: String, limit: Int): String {
        val selected = filters.firstOrNull()
        val suffix = when (selected) {
            "tracks", "users", "playlists" -> "/$selected"
            else -> ""
        }
        return "https://api-v2.soundcloud.com/search$suffix?q=${FormUrlEncoder.encode(query)}" +
            "&client_id=$clientId&limit=$limit&offset=0"
    }
}
