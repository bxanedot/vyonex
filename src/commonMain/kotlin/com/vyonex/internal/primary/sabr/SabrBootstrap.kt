// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

data class SabrFormatId(
    val itag: Int,
    val lastModified: Long = 0,
    val xtags: String? = null,
)

data class SabrBootstrap(
    val videoId: String,
    val serverAbrStreamingUrl: String,
    val videoPlaybackUstreamerConfig: ByteArray,
    val clientName: Int,
    val clientVersion: String,
    val audioFormat: SabrFormatId,
    val discardVideoFormat: SabrFormatId,
    val discardVideoHeight: Int,
    val selectedVideoFormat: SabrFormatId? = null,
    val selectedVideoWidth: Int? = null,
    val selectedVideoHeight: Int? = null,
    val selectedVideoContentLengthBytes: Long? = null,
    val selectedVideoMimeType: String? = null,
    val selectedVideoBitrate: Int? = null,
    val durationMs: Long,
    val contentLengthBytes: Long?,
    val mimeType: String,
    val audioTrackId: String? = null,
    val isDrc: Boolean = false,
    val poToken: ByteArray? = null,
    val requestUserAgent: String? = null,
    val requestOrigin: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is SabrBootstrap &&
            videoId == other.videoId &&
            serverAbrStreamingUrl == other.serverAbrStreamingUrl &&
            videoPlaybackUstreamerConfig.contentEquals(other.videoPlaybackUstreamerConfig) &&
            clientName == other.clientName &&
            clientVersion == other.clientVersion &&
            audioFormat == other.audioFormat &&
            discardVideoFormat == other.discardVideoFormat &&
            discardVideoHeight == other.discardVideoHeight &&
            selectedVideoFormat == other.selectedVideoFormat &&
            selectedVideoWidth == other.selectedVideoWidth &&
            selectedVideoHeight == other.selectedVideoHeight &&
            selectedVideoContentLengthBytes == other.selectedVideoContentLengthBytes &&
            selectedVideoMimeType == other.selectedVideoMimeType &&
            selectedVideoBitrate == other.selectedVideoBitrate &&
            durationMs == other.durationMs &&
            contentLengthBytes == other.contentLengthBytes &&
            mimeType == other.mimeType &&
            audioTrackId == other.audioTrackId &&
            isDrc == other.isDrc &&
            requestUserAgent == other.requestUserAgent &&
            requestOrigin == other.requestOrigin &&
            if (poToken == null) other.poToken == null else other.poToken != null && poToken.contentEquals(other.poToken)

    override fun hashCode(): Int =
        listOf(
            videoId,
            serverAbrStreamingUrl,
            videoPlaybackUstreamerConfig.contentHashCode(),
            clientName,
            clientVersion,
            audioFormat,
            discardVideoFormat,
            discardVideoHeight,
            selectedVideoFormat,
            selectedVideoWidth,
            selectedVideoHeight,
            selectedVideoContentLengthBytes,
            selectedVideoMimeType,
            selectedVideoBitrate,
            durationMs,
            contentLengthBytes,
            mimeType,
            audioTrackId,
            isDrc,
            poToken?.contentHashCode(),
            requestUserAgent,
            requestOrigin,
        ).hashCode()
}
