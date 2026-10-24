// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

internal data class PeertubeMediaFile(
    val fileUrl: String?,
    val fileDownloadUrl: String?,
    val torrentUrl: String?,
    val resolutionId: Int,
    val resolutionLabel: String?,
    val bitrate: Int,
    val hasAudio: Boolean,
)

internal data class PeertubeMediaPlaylist(
    val playlistUrl: String?,
    val files: List<PeertubeMediaFile>,
)

internal enum class PeertubeMediaDelivery { PROGRESSIVE_HTTP, HLS, TORRENT }

internal data class PeertubeMediaCandidate(
    val id: String,
    val url: String,
    val formatSuffix: String? = null,
    val isAudio: Boolean,
    val delivery: PeertubeMediaDelivery,
    val manifestUrl: String? = null,
    val bitrate: Int = 0,
    val isVideoOnly: Boolean = false,
    val resolution: String = "",
)

internal data class PeertubeMediaStreams(
    val audio: List<PeertubeMediaCandidate>,
    val video: List<PeertubeMediaCandidate>,
)

internal object PeertubeMediaMapper {
    fun map(
        files: List<PeertubeMediaFile>,
        playlists: List<PeertubeMediaPlaylist>,
        isLive: Boolean,
    ): PeertubeMediaStreams {
        val audio = mutableListOf<PeertubeMediaCandidate>()
        val video = mutableListOf<PeertubeMediaCandidate>()

        fun addFile(
            file: PeertubeMediaFile,
            delivery: PeertubeMediaDelivery,
            manifest: String? = null,
        ) {
            val label = file.resolutionLabel?.takeIf(String::isNotBlank)
                ?: file.resolutionId.takeIf { it > 0 }?.let { "${it}p" }
                ?: "unknown"
            for ((url, suffix) in listOf(
                file.fileUrl?.takeIf(String::isNotBlank) to "",
                file.fileDownloadUrl?.takeIf(String::isNotBlank) to "-fileDownloadUrl",
            )) {
                if (url == null || (suffix.isNotEmpty() && url == file.fileUrl)) continue
                val format = formatOf(url)
                val isAudio = file.resolutionId == 0 || label.equals("audio", ignoreCase = true)
                    || format in AUDIO_FORMATS
                val candidate = PeertubeMediaCandidate(
                    id = if (delivery == PeertubeMediaDelivery.HLS) "$label-hls$suffix"
                        else "$label-${format ?: "stream"}$suffix",
                    url = url,
                    formatSuffix = format,
                    isAudio = isAudio,
                    delivery = delivery,
                    manifestUrl = manifest,
                    bitrate = file.bitrate,
                    isVideoOnly = !file.hasAudio,
                    resolution = label,
                )
                if (isAudio) audio.add(candidate) else video.add(candidate)
            }
        }

        files.forEach { file ->
            addFile(file, PeertubeMediaDelivery.PROGRESSIVE_HTTP)
            file.torrentUrl?.takeIf(String::isNotBlank)?.let { torrent ->
                val label = file.resolutionLabel ?: "unknown"
                video.add(
                    PeertubeMediaCandidate(
                        id = "$label-torrent",
                        url = torrent,
                        isAudio = false,
                        delivery = PeertubeMediaDelivery.TORRENT,
                        resolution = label,
                    ),
                )
            }
        }

        for (playlist in playlists) {
            val manifest = playlist.playlistUrl?.takeIf(String::isNotBlank) ?: continue
            val before = audio.size + video.size
            playlist.files.forEach { addFile(it, PeertubeMediaDelivery.HLS, manifest) }
            if (before == audio.size + video.size) {
                video.add(
                    PeertubeMediaCandidate(
                        id = if (isLive) "live-hls" else "hls",
                        url = manifest,
                        isAudio = false,
                        delivery = PeertubeMediaDelivery.HLS,
                        manifestUrl = manifest,
                    ),
                )
            }
        }

        return PeertubeMediaStreams(audio.toList(), video.toList())
    }

    private fun formatOf(url: String): String? = url.substringBefore('?').substringBefore('#')
        .substringAfterLast('.', "").lowercase().takeIf { it in SUPPORTED_FORMATS }

    private val AUDIO_FORMATS = setOf(
        "m4a", "mp3", "mp2", "opus", "ogg", "aiff", "aif", "wav", "flac", "alac",
    )
    private val SUPPORTED_FORMATS = AUDIO_FORMATS + setOf(
        "mp4", "3gp", "webm", "vtt", "ttml", "srv1", "srv2", "srv3", "srt",
    )
}
