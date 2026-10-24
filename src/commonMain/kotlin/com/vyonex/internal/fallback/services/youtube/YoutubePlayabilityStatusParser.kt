// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube

internal enum class YoutubePlayabilityFailure {
    AGE_RESTRICTED,
    PRIVATE,
    BOT_CONFIRMATION,
    MUSIC_PREMIUM,
    PAID,
    MEMBERS_ONLY,
    COUNTRY_RESTRICTED,
    ACCOUNT_TERMINATED,
    UNAVAILABLE,
}

internal object YoutubePlayabilityStatusParser {
    fun classify(status: String?, reason: String?): YoutubePlayabilityFailure? {
        if (status == null || status.equals("ok", ignoreCase = true)) return null
        val normalizedStatus = status.lowercase()
        val normalizedReason = reason.orEmpty().lowercase()
        val canBePlaybackFailure = normalizedStatus == "unplayable" || normalizedStatus == "error"
        return when {
            normalizedStatus == "login_required" && normalizedReason.contains("inappropriate for some users") ->
                YoutubePlayabilityFailure.AGE_RESTRICTED
            normalizedStatus == "login_required" && normalizedReason.contains("private") ->
                YoutubePlayabilityFailure.PRIVATE
            normalizedStatus == "login_required" && normalizedReason.contains("a bot") ->
                YoutubePlayabilityFailure.BOT_CONFIRMATION
            canBePlaybackFailure && normalizedReason.contains("music premium") ->
                YoutubePlayabilityFailure.MUSIC_PREMIUM
            canBePlaybackFailure && normalizedReason.contains("payment") ->
                YoutubePlayabilityFailure.PAID
            canBePlaybackFailure && normalizedReason.contains("members") ->
                YoutubePlayabilityFailure.MEMBERS_ONLY
            canBePlaybackFailure && normalizedReason.contains("country") ->
                YoutubePlayabilityFailure.COUNTRY_RESTRICTED
            canBePlaybackFailure &&
                (normalizedReason.contains("closed") || normalizedReason.contains("terminated")) ->
                YoutubePlayabilityFailure.ACCOUNT_TERMINATED
            else -> YoutubePlayabilityFailure.UNAVAILABLE
        }
    }
}
