// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.soundcloud

internal data class SoundcloudTranscoding(
    val endpoint: String?,
    val protocol: String,
    val preset: String,
)

internal enum class SoundcloudAudioFormat(val bitrate: Int) {
    MP3(128),
    OPUS(64),
    M4A(160),
}

internal data class SoundcloudAudioCandidate(
    val endpoint: String,
    val preset: String,
    val format: SoundcloudAudioFormat,
    val hls: Boolean,
)

internal object SoundcloudTranscodingMapper {
    fun map(
        streamable: Boolean,
        transcodings: List<SoundcloudTranscoding>,
    ): List<SoundcloudAudioCandidate> {
        if (!streamable) return emptyList()
        return transcodings.mapNotNull { transcoding ->
            val endpoint = transcoding.endpoint?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            if (transcoding.protocol.contains("encrypted")) return@mapNotNull null
            val format = when {
                transcoding.preset.contains("mp3") -> SoundcloudAudioFormat.MP3
                transcoding.preset.contains("opus") -> SoundcloudAudioFormat.OPUS
                transcoding.preset.contains("aac_160k") -> SoundcloudAudioFormat.M4A
                else -> null
            } ?: return@mapNotNull null
            SoundcloudAudioCandidate(
                endpoint = endpoint,
                preset = transcoding.preset,
                format = format,
                hls = transcoding.protocol == "hls",
            )
        }
    }
}
