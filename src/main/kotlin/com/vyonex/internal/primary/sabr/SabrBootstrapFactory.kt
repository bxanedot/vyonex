// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

import com.vyonex.internal.primary.models.YouTubeClient
import com.vyonex.internal.primary.models.response.PlayerResponse
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@ExperimentalSabrApi
fun PlayerResponse.toSabrBootstrap(
    client: YouTubeClient,
    audioFormat: PlayerResponse.StreamingData.Format,
    poToken: String? = null,
    videoFormat: PlayerResponse.StreamingData.Format? = null,
): SabrBootstrap {
    require(client.useSabr) { "Selected player client is not configured for SABR" }
    return toSabrBootstrap(
        clientId = client.clientId.toInt(),
        clientVersion = client.clientVersion,
        audioFormat = audioFormat,
        poToken = poToken,
        requestUserAgent = client.userAgent,
        requestOrigin = sabrRequestOrigin(client.clientName),
        videoFormat = videoFormat,
    )
}

fun PlayerResponse.toSabrBootstrap(
    clientId: Int,
    clientVersion: String,
    audioFormat: PlayerResponse.StreamingData.Format,
    poToken: String? = null,
    requestUserAgent: String? = null,
    requestOrigin: String? = null,
    serverAbrStreamingUrlOverride: String? = null,
    videoFormat: PlayerResponse.StreamingData.Format? = null,
): SabrBootstrap {
    require(clientId > 0) { "SABR client ID must be positive" }
    val streaming = requireNotNull(streamingData) { "Player response has no streamingData" }
    val endpoint = serverAbrStreamingUrlOverride ?: requireNotNull(streaming.serverAbrStreamingUrl) {
        "Player response has no serverAbrStreamingUrl"
    }
    requireAllowedSabrUrl(endpoint)

    val encodedConfig = requireNotNull(playerConfig?.mediaCommonConfig?.mediaUstreamerRequestConfig?.videoPlaybackUstreamerConfig) {
        "Player response has no videoPlaybackUstreamerConfig"
    }
    val discardVideo =
        streaming.adaptiveFormats
            .asSequence()
            .filterNot { it.isAudio }
            .filter { it.height != null }
            .minWithOrNull(compareBy({ it.height }, { it.bitrate }))
            ?: error("Player response has no SABR video format to discard")

    val durationMs =
        audioFormat.approxDurationMs?.toLongOrNull()
            ?: videoDetails?.lengthSeconds?.toLongOrNull()?.let { seconds ->
                runCatching { Math.multiplyExact(seconds, 1_000L) }.getOrNull()
            }
            ?: error("Player response has no duration")
    require(durationMs > 0) { "Player response has an invalid duration" }

    val audioId = audioFormat.toSabrFormatId()
    val discardedVideoId = discardVideo.toSabrFormatId()
    val selectedVideoId = videoFormat?.toSabrFormatId()
    require(audioId.lastModified > 0) { "SABR audio format has no last-modified identity" }
    require(discardedVideoId.lastModified > 0) { "SABR video format has no last-modified identity" }
    require(selectedVideoId == null || selectedVideoId.lastModified > 0) {
        "Selected SABR video format has no last-modified identity"
    }

    return SabrBootstrap(
        videoId = requireNotNull(videoDetails?.videoId) { "Player response has no video ID" },
        serverAbrStreamingUrl = endpoint,
        videoPlaybackUstreamerConfig = decodePlayerBytes(encodedConfig, "videoPlaybackUstreamerConfig", MAX_USTREAMER_CONFIG_BYTES),
        clientName = clientId,
        clientVersion = clientVersion,
        audioFormat = audioId,
        discardVideoFormat = discardedVideoId,
        discardVideoHeight = discardVideo.height ?: 144,
        selectedVideoFormat = selectedVideoId,
        selectedVideoWidth = videoFormat?.width,
        selectedVideoHeight = videoFormat?.height,
        selectedVideoContentLengthBytes = videoFormat?.contentLength,
        selectedVideoMimeType = videoFormat?.mimeType?.substringBefore(';')?.trim(),
        selectedVideoBitrate = videoFormat?.bitrate,
        durationMs = durationMs,
        contentLengthBytes = audioFormat.contentLength,
        mimeType = audioFormat.mimeType.substringBefore(';').trim(),
        audioTrackId = audioFormat.audioTrack?.id,
        isDrc = audioFormat.isDrc,
        poToken = poToken?.takeIf(String::isNotBlank)?.let { decodePlayerBytes(it, "poToken", MAX_PO_TOKEN_BYTES) },
        requestUserAgent = requestUserAgent,
        requestOrigin = requestOrigin,
    )
}

fun sabrRequestOrigin(clientName: String): String? =
    when (clientName) {
        "WEB_REMIX" -> "https://music.youtube.com"
        "MWEB" -> "https://m.youtube.com"
        "WEB" -> "https://www.youtube.com"
        else -> null
    }

internal fun SabrFormatId.matches(other: SabrFormatId): Boolean =
    itag == other.itag &&
        (other.lastModified == 0L || lastModified == other.lastModified) &&
        (other.xtags.isNullOrEmpty() || xtags == other.xtags)

fun requireAllowedSabrUrl(value: String): String {
    val url = runCatching { Url(value) }.getOrElse { throw SabrProtocolException("Invalid SABR streaming URL") }
    val host = url.host.lowercase()
    val allowedHost = host == "googlevideo.com" || host.endsWith(".googlevideo.com")
    if (
        url.protocol != URLProtocol.HTTPS ||
        url.port != 443 ||
        !allowedHost ||
        url.encodedPath != "/videoplayback" ||
        url.user != null ||
        url.password != null
    ) {
        throw SabrProtocolException("SABR streaming URL is not an HTTPS googlevideo endpoint")
    }
    return value
}

private fun PlayerResponse.StreamingData.Format.toSabrFormatId() =
    SabrFormatId(itag, lastModified?.toLongOrNull() ?: 0, xtags)

@OptIn(ExperimentalEncodingApi::class)
private fun decodePlayerBytes(value: String, field: String, maximumBytes: Int): ByteArray {
    val maxEncodedLength = ((maximumBytes + 2) / 3) * 4
    if (value.length > maxEncodedLength) throw SabrProtocolException("Player response $field exceeded the size limit")
    val normalized = value.replace('-', '+').replace('_', '/').let { source ->
        source.padEnd(source.length + (4 - source.length % 4) % 4, '=')
    }
    val decoded = runCatching { Base64.decode(normalized) }
        .getOrElse { throw SabrProtocolException("Player response has an invalid $field") }
    if (decoded.size > maximumBytes) throw SabrProtocolException("Player response $field exceeded the size limit")
    return decoded
}

private const val MAX_USTREAMER_CONFIG_BYTES = 64 * 1024
private const val MAX_PO_TOKEN_BYTES = 16 * 1024
