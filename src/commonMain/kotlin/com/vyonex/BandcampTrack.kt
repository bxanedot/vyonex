// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// VyonEx — by bxane
package com.vyonex

import com.vyonex.internal.fallback.services.bandcamp.BandcampTrackPagePayload

/** Public Bandcamp track page metadata and its direct audio URL. */
data class BandcampTrack(
    val title: String?,
    val url: String?,
    val artist: String?,
    val publishDate: String?,
    val artId: Long,
    val description: String,
    val audioUrl: String,
    val durationSeconds: Double,
)

internal fun BandcampTrackPagePayload.toPublicTrack() = BandcampTrack(
    title = title,
    url = url,
    artist = artist,
    publishDate = publishDate,
    artId = artId,
    description = description,
    audioUrl = requireNotNull(audioUrl),
    durationSeconds = durationSeconds,
)
