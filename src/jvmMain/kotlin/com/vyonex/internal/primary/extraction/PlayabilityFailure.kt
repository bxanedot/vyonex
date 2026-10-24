// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import com.vyonex.internal.primary.extraction.strategy.ClientFailureKind

internal data class PlayabilityFailure(
    val status: String?,
    val reason: String?,
    val category: ClientFailureKind = classifyPlayabilityFailure(status, reason),
)

internal data class PlayerResponseBatch(
    val playableResponses: List<ClientResult>,
    val failures: List<PlayabilityFailure>,
    val requestFailures: List<Throwable>,
    val attempts: List<StreamAttemptDiagnostic>,
)

internal fun classifyPlayabilityFailures(
    failures: List<PlayabilityFailure>,
): StreamResolveException.Reason? = when {
    failures.any(PlayabilityFailure::isAgeRestricted) -> StreamResolveException.Reason.AGE_RESTRICTED
    failures.any(PlayabilityFailure::isUnavailable) -> StreamResolveException.Reason.UNAVAILABLE
    else -> null
}

private fun classifyPlayabilityFailure(status: String?, reason: String?): ClientFailureKind {
    val normalized = status.orEmpty().uppercase()
    return when {
        normalized in AGE_STATUSES || normalized in UNAVAILABLE_STATUSES ->
            ClientFailureKind.PLAYABILITY
        normalized == "LOGIN_REQUIRED" -> ClientFailureKind.TOKEN
        else -> ClientFailureKind.PLAYABILITY
    }
}

private fun PlayabilityFailure.isAgeRestricted(): Boolean {
    val normalizedStatus = status.orEmpty().uppercase()
    val normalizedReason = reason.orEmpty().lowercase()
    return normalizedStatus in AGE_STATUSES || AGE_PHRASES.any(normalizedReason::contains)
}

private fun PlayabilityFailure.isUnavailable(): Boolean {
    val normalizedStatus = status.orEmpty().uppercase()
    val normalizedReason = reason.orEmpty().lowercase()
    return normalizedStatus in UNAVAILABLE_STATUSES || UNAVAILABLE_PHRASES.any(normalizedReason::contains)
}

private val AGE_STATUSES = setOf(
    "AGE_CHECK_REQUIRED",
    "AGE_VERIFICATION_REQUIRED",
    "CONTENT_CHECK_REQUIRED",
)
private val UNAVAILABLE_STATUSES = setOf("UNPLAYABLE", "VIDEO_UNAVAILABLE", "LIVE_STREAM_OFFLINE")
private val AGE_PHRASES = listOf("confirm your age", "age-restricted", "age restricted", "age verification")
private val UNAVAILABLE_PHRASES = listOf("video unavailable", "not available", "private video", "has been removed")
