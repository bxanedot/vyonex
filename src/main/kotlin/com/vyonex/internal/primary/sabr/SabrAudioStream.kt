// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

import com.vyonex.internal.primary.InnerTubeLogger
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Streams one fixed audio representation from the SABR endpoint. */
@ExperimentalSabrApi
class SabrAudioStream(
    httpClient: HttpClient,
    bootstrap: SabrBootstrap,
    maxRequestsWithoutProgress: Int = 3,
    playbackPositionMs: (() -> Long)? = null,
    onResponse: ((SabrResponseDiagnostics) -> Unit)? = null,
    requestRetryDelay: suspend (Long) -> Unit = { delay(it) },
    initialPlayerTimeMs: Long = 0,
    logger: InnerTubeLogger = InnerTubeLogger.NONE,
) {
    private val stream =
        SabrStreamEngine(
            httpClient,
            bootstrap,
            SabrMediaType.AUDIO,
            maxRequestsWithoutProgress,
            playbackPositionMs,
            onResponse,
            requestRetryDelay,
            initialPlayerTimeMs,
            logger,
        )

    fun bytes(): Flow<ByteArray> = chunks().map { it.data }

    fun chunks(): Flow<SabrChunk> = stream.chunks()
}

/** Streams one fixed video representation in step with the caller's playback clock. */
@ExperimentalSabrApi
class SabrVideoStream(
    httpClient: HttpClient,
    bootstrap: SabrBootstrap,
    maxRequestsWithoutProgress: Int = 3,
    playbackPositionMs: (() -> Long)? = null,
    onResponse: ((SabrResponseDiagnostics) -> Unit)? = null,
    requestRetryDelay: suspend (Long) -> Unit = { delay(it) },
    initialPlayerTimeMs: Long = 0,
    logger: InnerTubeLogger = InnerTubeLogger.NONE,
) {
    private val stream =
        SabrStreamEngine(
            httpClient,
            bootstrap,
            SabrMediaType.VIDEO,
            maxRequestsWithoutProgress,
            playbackPositionMs,
            onResponse,
            requestRetryDelay,
            initialPlayerTimeMs,
            logger,
        )

    fun bytes(): Flow<ByteArray> = chunks().map { it.data }

    fun chunks(): Flow<SabrChunk> = stream.chunks()
}
