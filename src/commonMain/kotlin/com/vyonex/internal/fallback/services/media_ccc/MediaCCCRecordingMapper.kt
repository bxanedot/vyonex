// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc

internal data class MediaCCCRecordingCandidate(
    val filename: String?,
    val url: String?,
    val language: String?,
    val height: Int?,
    val formatSuffix: String?,
)

internal data class MediaCCCRecordingStreams(
    val audio: List<MediaCCCRecordingCandidate>,
    val video: List<MediaCCCRecordingCandidate>,
)

internal object MediaCCCRecordingMapper {
    fun map(recordings: List<MediaCCCRecordingPayload>?): MediaCCCRecordingStreams {
        val audio = mutableListOf<MediaCCCRecordingCandidate>()
        val video = mutableListOf<MediaCCCRecordingCandidate>()
        for (recording in recordings.orEmpty()) {
            val mimeType = recording.mimeType.orEmpty()
            val target = when {
                mimeType.startsWith("audio") -> audio
                mimeType.startsWith("video") -> video
                else -> continue
            }
            target.add(
                MediaCCCRecordingCandidate(
                    filename = recording.filename,
                    url = recording.recordingUrl,
                    language = recording.language,
                    height = recording.height,
                    formatSuffix = formatSuffix(mimeType),
                ),
            )
        }
        return MediaCCCRecordingStreams(audio, video)
    }

    private fun formatSuffix(mimeType: String): String? = when {
        mimeType.startsWith("audio") && mimeType.endsWith("opus") -> "opus"
        mimeType.startsWith("audio") && mimeType.endsWith("mpeg") -> "mp3"
        mimeType.startsWith("audio") && mimeType.endsWith("ogg") -> "ogg"
        mimeType.startsWith("video") && mimeType.endsWith("webm") -> "webm"
        mimeType.startsWith("video") && mimeType.endsWith("mp4") -> "mp4"
        else -> null
    }
}
