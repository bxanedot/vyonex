// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import com.vyonex.internal.primary.models.response.PlayerResponse.StreamingData.Format

public fun selectBestAudioFormat(
    formats: List<Format>,
    audioQuality: AudioQuality = AudioQuality.AUTO,
    requireUrl: Boolean = true,
): Format? {
    val candidates = formats.filter { !requireUrl || !it.url.isNullOrBlank() }
    if (candidates.isEmpty()) return null

    return when (audioQuality) {
        AudioQuality.LOW ->
            candidates.filter { it.mimeType.contains("audio/mp4") }
                .minByOrNull { it.bitrate }
                ?: candidates.minByOrNull { it.bitrate }

        AudioQuality.AUTO -> {
            val webm = candidates.filter { it.mimeType.contains("audio/webm") }
            (webm.ifEmpty { candidates }).maxByOrNull(::audioQualityScore)
        }

        AudioQuality.HIGH -> candidates.maxWithOrNull(highAudioComparator)
        AudioQuality.MP4 ->
            candidates.filter { it.mimeType.contains("audio/mp4") }
                .maxByOrNull(::audioQualityScore)
    }
}

public fun selectBestVideoFormat(
    formats: List<Format>,
    requireUrl: Boolean = true,
    maxHeight: Int = 2160,
): Format? {
    val videoFormats =
        formats.filter {
            (!requireUrl || !it.url.isNullOrBlank()) &&
                it.width != null &&
                (it.height ?: 0) > 0
        }
    if (videoFormats.isEmpty()) return null

    val withinCap = videoFormats.filter { (it.height ?: 0) <= maxHeight }
    if (withinCap.isNotEmpty()) return withinCap.maxWithOrNull(videoQualityComparator)

    val lowestAvailableHeight = videoFormats.minOf { it.height ?: 0 }
    return videoFormats.filter { it.height == lowestAvailableHeight }.maxWithOrNull(videoQualityComparator)
}

private fun audioQualityScore(format: Format): Long {
    val codecPreference =
        when {
            format.mimeType.contains("audio/webm") -> 100
            format.mimeType.contains("audio/mp4") -> 50
            else -> 0
        }
    val channelPreference =
        when (format.audioChannels) {
            2 -> 50_000
            1 -> 0
            else -> 25_000
        }
    return codecPreference * 1_000_000L +
        channelPreference +
        format.bitrate +
        (format.audioSampleRate ?: 0).coerceIn(0, 48_000) / 10
}

private val highAudioComparator =
    compareBy<Format> { it.bitrate }
        .thenBy {
            when (it.audioChannels) {
                2 -> 2
                null -> 1
                else -> 0
            }
        }.thenBy { (it.audioSampleRate ?: 0).coerceIn(0, 48_000) }
        .thenBy {
            when {
                it.mimeType.contains("audio/webm") -> 2
                it.mimeType.contains("audio/mp4") -> 1
                else -> 0
            }
        }

private val videoQualityComparator =
    compareBy<Format> { it.height ?: 0 }
        .thenBy {
            val mimeType = it.mimeType.lowercase()
            when {
                "av01" in mimeType -> 1
                "avc1" in mimeType || "vp09" in mimeType || "vp9" in mimeType ||
                    "video/mp4" in mimeType || "video/webm" in mimeType -> 2
                else -> 0
            }
        }.thenByDescending { it.bitrate.takeIf { bitrate -> bitrate > 0 } ?: Int.MAX_VALUE }
