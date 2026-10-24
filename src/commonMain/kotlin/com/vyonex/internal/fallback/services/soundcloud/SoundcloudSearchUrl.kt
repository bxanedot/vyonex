// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import com.vyonex.internal.url.FormUrlEncoder

internal object SoundcloudSearchUrl {
    private const val API_V2_BASE_URL = "https://api-v2.soundcloud.com/"

    fun build(query: String, filters: List<String>, clientId: String, limit: Int): String {
        val selected = filters.firstOrNull()
        val suffix = when (selected) {
            "tracks", "users", "playlists" -> "/$selected"
            else -> ""
        }
        return "${API_V2_BASE_URL}search$suffix?q=${FormUrlEncoder.encode(query)}" +
            "&client_id=$clientId&limit=$limit&offset=0"
    }

    fun suggestionsUrl(query: String, clientId: String): String =
        "${API_V2_BASE_URL}search/queries?q=${FormUrlEncoder.encode(query)}" +
            "&client_id=$clientId&limit=10"

    fun resolveUrl(url: String, clientId: String): String =
        "${API_V2_BASE_URL}resolve?url=${FormUrlEncoder.encode(url)}&client_id=$clientId"

    fun widgetResolveUrl(url: String, clientId: String): String =
        "https://api-widget.soundcloud.com/resolve?url=${FormUrlEncoder.encode(url)}" +
            "&format=json&client_id=$clientId"

    fun embedPlayerUrl(apiUrl: String): String =
        "https://w.soundcloud.com/player/?url=${FormUrlEncoder.encode(apiUrl)}"

    fun withClientId(nextUrl: String, clientId: String): String {
        if (nextUrl.isBlank() || nextUrl.contains("client_id=")) return nextUrl
        return "$nextUrl${if (nextUrl.contains('?')) '&' else '?'}client_id=$clientId"
    }
}
