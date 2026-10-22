// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import io.ktor.http.Url
import kotlin.random.Random

private const val NONCE_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
private val NONCE_PARAMETER = Regex("([?&])cpn=[^&#]*")

public fun generateClientPlaybackNonce(): String = buildString(16) {
    repeat(16) { append(NONCE_ALPHABET[Random.nextInt(NONCE_ALPHABET.length)]) }
}

public fun appendClientPlaybackNonce(url: String, clientPlaybackNonce: String): String {
    if (!isValidNonce(clientPlaybackNonce)) return url
    val parsed = parseApprovedMediaUrl(url) ?: return url
    if (parsed.parameters.contains("cpn")) return url
    return addQueryParameter(url, "cpn=$clientPlaybackNonce")
}

public fun replaceClientPlaybackNonce(url: String, clientPlaybackNonce: String): String {
    if (!isValidNonce(clientPlaybackNonce) || parseApprovedMediaUrl(url) == null) return url
    val existing = NONCE_PARAMETER.find(url) ?: return appendClientPlaybackNonce(url, clientPlaybackNonce)
    return url.replaceRange(
        existing.range,
        existing.groupValues[1] + "cpn=" + clientPlaybackNonce,
    )
}

private fun parseApprovedMediaUrl(value: String): Url? {
    val parsed = runCatching { Url(value) }.getOrNull() ?: return null
    return parsed.takeIf {
        it.protocol.name == "https" &&
            it.port == 443 &&
            (it.host == "googlevideo.com" || it.host.endsWith(".googlevideo.com")) &&
            it.encodedPath == "/videoplayback" &&
            it.user == null && it.password == null
    }
}

private fun addQueryParameter(url: String, parameter: String): String {
    val fragmentAt = url.indexOf('#')
    val query = if (fragmentAt < 0) url else url.substring(0, fragmentAt)
    val fragment = if (fragmentAt < 0) "" else url.substring(fragmentAt)
    val separator = if ('?' in query) "&" else "?"
    return "$query$separator$parameter$fragment"
}

private fun isValidNonce(value: String): Boolean =
    value.length == 16 && value.all { it in NONCE_ALPHABET }
