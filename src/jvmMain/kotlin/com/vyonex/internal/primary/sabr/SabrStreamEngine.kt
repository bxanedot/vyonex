// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

import com.vyonex.internal.primary.InnerTubeLogger
import com.vyonex.internal.primary.d
import com.vyonex.internal.primary.e
import com.vyonex.internal.primary.i
import com.vyonex.internal.primary.w
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseRedirectEvent
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow

/** Owns the request loop and response state for one selected SABR representation. */
internal class SabrStreamEngine(
    private val httpClient: HttpClient,
    private val bootstrap: SabrBootstrap,
    private val mediaType: SabrMediaType,
    private val maxRequestsWithoutProgress: Int,
    private val playbackPositionMs: (() -> Long)?,
    private val onResponse: ((SabrResponseDiagnostics) -> Unit)?,
    private val requestRetryDelay: suspend (Long) -> Unit,
    private val initialPlayerTimeMs: Long,
    private val logger: InnerTubeLogger,
) {
    private val format =
        when (mediaType) {
            SabrMediaType.AUDIO -> bootstrap.audioFormat
            SabrMediaType.VIDEO -> requireNotNull(bootstrap.selectedVideoFormat) { "SABR video format is not selected" }
        }
    private val label = mediaType.name.lowercase()
    private val logTag = if (mediaType == SabrMediaType.AUDIO) "SabrAudioStream" else "SabrVideoStream"
    private val expectedBytes =
        when (mediaType) {
            SabrMediaType.AUDIO -> bootstrap.contentLengthBytes
            SabrMediaType.VIDEO -> bootstrap.selectedVideoContentLengthBytes
        }

    fun chunks(): Flow<SabrChunk> {
        var requestCount = 0
        var lastStatus: Int? = null
        var lastContentType: String? = null
        var lastResponseBytes = 0L
        var lastPlayerTimeMs = initialPlayerTimeMs
        var lastPlaybackPositionMs: Long? = null

        return channelFlow {
            val session = Session()
            logger.i(
                logTag,
                "Opened SABR $label stream itag=${format.itag}, duration=${bootstrap.durationMs}ms, " +
                    "poToken=${presence(bootstrap.poToken)}, clientVersion=${bootstrap.clientVersion}.",
                mediaId = bootstrap.videoId,
                details = bootstrap.logDetails(format, mediaType),
            )

            while (!session.finished) {
                if (requestCount >= MAX_REQUEST_COUNT) {
                    throw SabrProtocolException("SABR exceeded $MAX_REQUEST_COUNT requests")
                }
                session.waitForPlaybackCapacity()
                if (session.backoffTimeMs > 0) delay(session.backoffTimeMs.coerceAtMost(MAX_BACKOFF_MS))

                val currentRequest = requestCount
                val requestPlayerTimeMs = session.playerTimeMs
                val observedPosition = playbackPositionMs?.invoke()?.coerceAtLeast(0L)
                lastPlayerTimeMs = requestPlayerTimeMs
                lastPlaybackPositionMs = observedPosition
                val body = session.requestBody(requestPlayerTimeMs)

                val retry =
                    httpClient.withRedirectsDisabled {
                        preparePost(requestUrl(session.streamingUrl, requestCount++)) {
                            attributes.put(SABR_REQUEST_ATTRIBUTE, Unit)
                            header(HttpHeaders.Accept, CONTENT_TYPE_UMP)
                            header(HttpHeaders.AcceptEncoding, "identity")
                            bootstrap.requestUserAgent?.let { header(HttpHeaders.UserAgent, it) }
                            bootstrap.requestOrigin?.let {
                                header(HttpHeaders.Origin, it)
                                header(HttpHeaders.Referrer, "$it/")
                            }
                            setBody(body)
                            contentType(ContentType.parse(CONTENT_TYPE_PROTOBUF))
                        }.execute { response ->
                            lastStatus = response.status.value
                            lastContentType = response.headers[HttpHeaders.ContentType]
                            lastResponseBytes = 0L
                            val channel = response.bodyAsChannel()

                            if (!response.status.isSuccess()) {
                                val details = responseDetails(response)
                                channel.cancel(null)
                                if (session.shouldRetry(response.status.value)) {
                                    session.consecutiveTransientFailures++
                                    session.transientRetryCount++
                                    logger.w(
                                        logTag,
                                        "SABR HTTP ${response.status.value} is transient; retry " +
                                            "${session.consecutiveTransientFailures}/$MAX_TRANSIENT_RETRIES.",
                                        mediaId = bootstrap.videoId,
                                        details = retryDetails(response.status.value, details, session.consecutiveTransientFailures),
                                    )
                                    requestRetryDelay(retryDelayMs(response, session.consecutiveTransientFailures))
                                    return@execute true
                                }
                                throw SabrProtocolException(
                                    "SABR request failed with HTTP ${response.status.value}" +
                                        details.takeIf(String::isNotEmpty)?.let { " ($it)" }.orEmpty(),
                                    httpStatusCode = response.status.value,
                                )
                            }

                            val contentType = response.headers[HttpHeaders.ContentType].orEmpty()
                            if (!contentType.startsWith(CONTENT_TYPE_UMP)) {
                                channel.cancel(null)
                                throw SabrProtocolException("Unexpected SABR content type")
                            }
                            session.consecutiveTransientFailures = 0

                            val processor = SabrUmpProcessor(shouldCollectMedia = ::isSelectedHeader)
                            val responseState = ResponseState(session, requestPlayerTimeMs, observedPosition, currentRequest)
                            val buffer = ByteArray(READ_BUFFER_SIZE)
                            try {
                                while (true) {
                                    val count = channel.readAvailable(buffer, 0, buffer.size)
                                    if (count == -1) break
                                    if (count == 0) continue
                                    responseState.addBytes(count)
                                    lastResponseBytes = responseState.responseBytes
                                    processor.feed(buffer, count).forEach { event -> responseState.accept(event) { send(it) } }
                                }
                                processor.finish()
                                responseState.finish { send(it) }
                            } catch (cancelled: CancellationException) {
                                channel.cancel(cancelled)
                                throw cancelled
                            } catch (error: Throwable) {
                                channel.cancel(error)
                                throw error
                            }
                            false
                        }
                    }
                if (retry) continue
            }

            session.validateComplete()
            logger.i(
                logTag,
                "SABR $label stream finished after $requestCount UMP requests, " +
                    "${session.cumulativeSegmentCount} segments, ${session.cumulativeMediaBytes} bytes.",
                mediaId = bootstrap.videoId,
                details =
                    mapOf(
                        "requestCount" to requestCount.toString(),
                        "segments" to session.cumulativeSegmentCount.toString(),
                        "mediaBytes" to session.cumulativeMediaBytes.toString(),
                        "transientRetries" to session.transientRetryCount.toString(),
                        "protectionRetries" to session.protectionRetryCount.toString(),
                    ) + bootstrap.logDetails(format, mediaType),
            )
        }.buffer(0).catch { error ->
            if (error is CancellationException) throw error
            onResponse?.invoke(
                SabrResponseDiagnostics(
                    requestNumber = requestCount,
                    requestPlayerTimeMs = lastPlayerTimeMs,
                    observedPlaybackPositionMs = lastPlaybackPositionMs,
                    bufferedRanges = emptyList(),
                    receivedSequences = emptyList(),
                    receivedStartRanges = emptyList(),
                    receivedContentLengths = emptyList(),
                    initializationReceived = false,
                    playbackCookieSize = 0,
                    contextTypes = emptySet(),
                    activeContextTypes = emptySet(),
                    targetAudioReadaheadMs = 0L,
                    minAudioReadaheadMs = 0L,
                    maxTimeSinceLastRequestMs = 0L,
                    backoffTimeMs = 0L,
                    responseBytes = lastResponseBytes,
                    httpStatus = lastStatus,
                    contentType = lastContentType,
                    failureCategory = error.findSabrProtocolException()?.kind?.name ?: error::class.simpleName,
                    failureMessage = "SABR stream failed",
                ),
            )
            logger.e(
                logTag,
                "SABR $label stream failed",
                mediaId = bootstrap.videoId,
                details =
                    bootstrap.logDetails(format, mediaType) +
                        buildMap {
                            put("failureType", error::class.simpleName ?: "Throwable")
                            error.findSabrProtocolException()?.let { failure ->
                                put("sabrKind", failure.kind.name)
                                failure.httpStatusCode?.let { put("httpStatus", it.toString()) }
                            }
                        },
            )
            throw error
        }
    }

    private inner class Session {
        var streamingUrl = requireAllowedSabrUrl(bootstrap.serverAbrStreamingUrl)
        var playerTimeMs = initialPlayerTimeMs.coerceIn(0L, (bootstrap.durationMs - 1L).coerceAtLeast(0L))
        var initialized = false
        var initialization: SabrFormatInitialization? = null
        var initSegmentEmitted = false
        var playbackCookie: ByteArray? = null
        var targetAudioReadaheadMs = 0L
        var maxTimeSinceLastRequestMs = 0L
        var minAudioReadaheadMs = 0L
        var bufferedRanges = emptyList<SabrBufferedRange>()
        var backoffTimeMs = 0L
        var requestsWithoutProgress = 0
        var ended = false
        var firstSequenceNumber: Int? = null
        var lastSequenceNumber: Int? = null
        var seekStartEstablished = false
        var consecutiveTransientFailures = 0
        var transientRetryCount = 0
        var cumulativeMediaBytes = 0L
        var cumulativeStreamBytes = 0L
        var cumulativeSegmentCount = 0
        var protectionPending = false
        var protectionRetryCount = 0
        var protectionRetryLimit: Int? = null
        val contexts = mutableMapOf<Int, SabrContext>()
        val activeContextTypes = mutableSetOf<Int>()
        var retainedContextBytes = 0L

        val finished: Boolean
            get() =
                ended ||
                    (initialization?.endSegmentNumber?.let { end ->
                        lastSequenceNumber?.let { it >= end } == true
                    } == true) ||
                    playerTimeMs >= expectedDurationMs()

        fun requestBody(playerTimeMs: Long): ByteArray =
            SabrProtoCodec.encodeRequest(
                bootstrap = bootstrap,
                playerTimeMs = playerTimeMs,
                initialized = initialized,
                bufferedRanges = bufferedRanges,
                playbackCookie = playbackCookie,
                contexts = contexts.values,
                activeContextTypes = activeContextTypes,
                mediaType = mediaType,
            )

        fun shouldRetry(status: Int): Boolean = status in TRANSIENT_HTTP_STATUSES && consecutiveTransientFailures < MAX_TRANSIENT_RETRIES

        suspend fun waitForPlaybackCapacity() {
            if (playbackPositionMs == null || targetAudioReadaheadMs <= 0L || playerTimeMs <= 0L) return
            val limit = heartbeatLimit(maxTimeSinceLastRequestMs)
            var waited = 0L
            while (waited < limit) {
                val position = playbackPositionMs.invoke().coerceAtLeast(0L)
                if (playerTimeMs - position <= targetAudioReadaheadMs) return
                delay(PLAYBACK_POLL_INTERVAL_MS)
                waited += PLAYBACK_POLL_INTERVAL_MS
            }
        }

        fun expectedDurationMs(): Long = initialization?.durationMs?.takeIf { it > 0L } ?: bootstrap.durationMs

        fun validateComplete() {
            if (!initSegmentEmitted) throw SabrProtocolException("SABR stream has no initialization segment")
            val finalSequence = lastSequenceNumber ?: throw SabrProtocolException("SABR stream has no media segments")
            initialization?.endSegmentNumber?.let { expected ->
                if (finalSequence != expected) {
                    throw SabrProtocolException("Incomplete SABR stream: last segment is $finalSequence, expected $expected")
                }
            }

            val duration = expectedDurationMs()
            val tolerance = maxOf(MIN_DURATION_TOLERANCE_MS, duration / 100L)
            if (absoluteDifference(playerTimeMs, duration) > tolerance) {
                throw SabrProtocolException("Incomplete SABR stream duration: downloaded ${playerTimeMs}ms, expected ${duration}ms")
            }
            if (initialPlayerTimeMs == 0L) {
                expectedBytes?.takeIf { it > 0L }?.let { expected ->
                    if (cumulativeStreamBytes != expected) {
                        throw SabrProtocolException("Incomplete SABR stream bytes: emitted $cumulativeStreamBytes, expected $expected")
                    }
                }
            }
        }
    }

    private inner class ResponseState(
        private val session: Session,
        private val requestPlayerTimeMs: Long,
        private val observedPosition: Long?,
        private val requestNumber: Int,
    ) {
        var responseBytes = 0L
            private set
        private var mediaBytes = 0L
        private var initializationReceived = false
        private var initializationCount = 0
        private val receivedSequences = mutableListOf<Int>()
        private val receivedStartRanges = mutableListOf<Long>()
        private val receivedContentLengths = mutableListOf<Long>()
        private var unselectedHeaderCount = 0
        private var protectionStatus: Int? = null
        private var protectionMaxRetries: Int? = null
        private val pendingSegments = linkedMapOf<Int, SabrSegment>()
        private var firstNewHeader: SabrMediaHeader? = null
        private var lastNewHeader: SabrMediaHeader? = null
        private var newSegmentsDurationMs = 0L
        private var newSegmentsBytes = 0L
        private var newSegmentCount = 0

        fun addBytes(count: Int) {
            responseBytes += count
        }

        suspend fun accept(event: SabrEvent, emit: suspend (SabrChunk) -> Unit) {
            when (event) {
                is SabrEvent.MediaHeader -> onHeader(event.header)
                is SabrEvent.Segment -> onSegment(event.segment, emit)
                is SabrEvent.FormatInitialized -> onInitialization(event.initialization)
                is SabrEvent.NextRequestPolicy -> onPolicy(event)
                is SabrEvent.Redirect -> session.streamingUrl = requireAllowedSabrUrl(event.url)
                is SabrEvent.ContextUpdate -> onContextUpdate(event.context)
                is SabrEvent.ContextSendingPolicy -> onContextPolicy(event)
                is SabrEvent.StreamProtectionStatus -> {
                    protectionStatus = event.status
                    protectionMaxRetries = event.maxRetries
                }
                is SabrEvent.Error -> throw SabrProtocolException("SABR error ${event.type.orEmpty()} (${event.code})")
                SabrEvent.ReloadPlayerResponse ->
                    throw SabrProtocolException("SABR requested a fresh player response", SabrFailureKind.RELOAD_PLAYER)
                SabrEvent.EndOfTrack -> session.ended = true
            }
        }

        private fun onHeader(header: SabrMediaHeader) {
            if (!header.isInitSegment && isSelectedHeader(header)) {
                receivedSequences += header.sequenceNumber
                receivedStartRanges += header.startRange
                receivedContentLengths += header.contentLength
            } else if (!header.isInitSegment) {
                unselectedHeaderCount++
            }
        }

        private suspend fun onSegment(segment: SabrSegment, emit: suspend (SabrChunk) -> Unit) {
            mediaBytes += segment.data.size
            if (mediaBytes > MAX_RESPONSE_MEDIA_BYTES) {
                throw SabrProtocolException("SABR response media exceeded the memory limit")
            }
            if (segment.header.isInitSegment) {
                initializationReceived = true
                if (++initializationCount > 1) throw SabrProtocolException("SABR returned duplicate initialization segments")
                if (!session.initSegmentEmitted) {
                    emit(segment.toChunk())
                    session.initSegmentEmitted = true
                    session.cumulativeStreamBytes += segment.data.size
                    emitAvailableSegments(emit)
                }
                return
            }

            val sequence = segment.header.sequenceNumber
            val precedesSeek = session.seekStartEstablished && session.firstSequenceNumber?.let { sequence < it } == true
            val alreadySent =
                precedesSeek ||
                    session.lastSequenceNumber?.let { last ->
                        sequence >= checkNotNull(session.firstSequenceNumber) && sequence <= last
                    } == true
            if (alreadySent) return
            if (pendingSegments.put(sequence, segment) != null) {
                throw SabrProtocolException("SABR returned duplicate segment $sequence")
            }
            if (session.initSegmentEmitted) emitAvailableSegments(emit)
        }

        private suspend fun emitAvailableSegments(emit: suspend (SabrChunk) -> Unit) {
            val seekSequence = findSeekSequence()
            if (seekSequence != null) session.seekStartEstablished = true
            emitReady(seekSequence, establishFromMinimum = false, emit = emit)
        }

        private fun findSeekSequence(): Int? {
            if (session.firstSequenceNumber != null || requestPlayerTimeMs <= 0L) return null
            return pendingSegments.values.firstOrNull { segment ->
                segment.header.startMs <= requestPlayerTimeMs && requestPlayerTimeMs < segment.endTimeMs()
            }?.header?.sequenceNumber
        }

        private suspend fun emitReady(
            initialSequence: Int? = null,
            establishFromMinimum: Boolean,
            emit: suspend (SabrChunk) -> Unit,
        ) {
            if (session.lastSequenceNumber == null) {
                initialSequence?.let { seek -> pendingSegments.keys.filter { it < seek }.forEach(pendingSegments::remove) }
                val first =
                    initialSequence
                        ?: if (pendingSegments.containsKey(0) && (requestPlayerTimeMs <= 0L || establishFromMinimum)) {
                            0
                        } else if (establishFromMinimum) {
                            pendingSegments.keys.minOrNull()
                        } else {
                            null
                        } ?: return
                emitSegment(checkNotNull(pendingSegments.remove(first)), emit)
            }
            while (true) {
                val next = checkNotNull(session.lastSequenceNumber) + 1
                val segment = pendingSegments.remove(next) ?: return
                emitSegment(segment, emit)
            }
        }

        private suspend fun emitSegment(segment: SabrSegment, emit: suspend (SabrChunk) -> Unit) {
            val endTime = segment.endTimeMs()
            emit(segment.toChunk())
            if (session.firstSequenceNumber == null) session.firstSequenceNumber = segment.header.sequenceNumber
            session.lastSequenceNumber = segment.header.sequenceNumber
            session.playerTimeMs = maxOf(session.playerTimeMs, endTime)
            session.cumulativeMediaBytes += segment.data.size
            session.cumulativeStreamBytes += segment.data.size
            session.cumulativeSegmentCount++
            if (newSegmentsDurationMs > Long.MAX_VALUE - segment.header.durationMs) {
                throw SabrProtocolException("SABR buffered duration exceeded the supported range")
            }
            if (firstNewHeader == null) firstNewHeader = segment.header
            lastNewHeader = segment.header
            newSegmentsDurationMs += segment.header.durationMs
            newSegmentsBytes += segment.data.size
            newSegmentCount++
        }

        private fun onInitialization(value: SabrFormatInitialization?) {
            if (value?.formatId?.matches(format) == true) {
                session.initialized = true
                session.initialization = value
            }
        }

        private fun onPolicy(event: SabrEvent.NextRequestPolicy) {
            if ((event.playbackCookie?.size ?: 0) > MAX_PLAYBACK_COOKIE_BYTES) {
                throw SabrProtocolException("SABR playback cookie exceeded the memory limit")
            }
            session.targetAudioReadaheadMs = event.targetAudioReadaheadMs
            session.maxTimeSinceLastRequestMs = event.maxTimeSinceLastRequestMs
            session.backoffTimeMs = event.backoffTimeMs
            session.minAudioReadaheadMs = event.minAudioReadaheadMs
            session.playbackCookie = event.playbackCookie
        }

        private fun onContextUpdate(context: SabrContext) {
            val previous = session.contexts[context.type]
            if (context.writePolicy == KEEP_EXISTING && previous != null) return
            if (context.value.size > MAX_CONTEXT_VALUE_BYTES) throw SabrProtocolException("SABR context value exceeded the memory limit")
            if (previous == null && session.contexts.size >= MAX_CONTEXT_COUNT) {
                throw SabrProtocolException("SABR context count exceeded the memory limit")
            }
            val total = session.retainedContextBytes - (previous?.value?.size ?: 0) + context.value.size
            if (total > MAX_CONTEXT_BYTES) throw SabrProtocolException("SABR contexts exceeded the memory limit")
            session.contexts[context.type] = context
            session.retainedContextBytes = total
            if (context.sendByDefault) session.activeContextTypes += context.type
        }

        private fun onContextPolicy(event: SabrEvent.ContextSendingPolicy) {
            if (event.start.size > MAX_CONTEXT_COUNT || event.stop.size > MAX_CONTEXT_COUNT || event.discard.size > MAX_CONTEXT_COUNT) {
                throw SabrProtocolException("SABR context policy exceeded the type limit")
            }
            session.activeContextTypes += event.start
            session.activeContextTypes -= event.stop
            event.discard.forEach { type ->
                session.activeContextTypes -= type
                session.contexts.remove(type)?.let { session.retainedContextBytes -= it.value.size }
            }
            if (session.activeContextTypes.size > MAX_CONTEXT_COUNT) {
                throw SabrProtocolException("SABR active context count exceeded the memory limit")
            }
        }

        suspend fun finish(emit: suspend (SabrChunk) -> Unit) {
            emitReady(establishFromMinimum = true, emit = emit)
            if (pendingSegments.isNotEmpty()) {
                val expected = (session.lastSequenceNumber ?: -1) + 1
                throw SabrProtocolException(
                    "SABR segment sequence gap: expected $expected, received ${pendingSegments.keys.minOrNull()}",
                )
            }

            if (newSegmentCount > 0) session.requestsWithoutProgress = 0
            session.bufferedRanges =
                if (newSegmentCount == 0) {
                    emptyList()
                } else {
                    val first = checkNotNull(firstNewHeader)
                    val last = checkNotNull(lastNewHeader)
                    listOf(SabrBufferedRange(format, first.startMs, newSegmentsDurationMs, first.sequenceNumber, last.sequenceNumber))
                }
            updateProtectionState()

            val diagnostics = diagnostics()
            onResponse?.invoke(diagnostics)
            logResponse(diagnostics)
            failIfProtectionRequired()
            if (newSegmentCount == 0 && !session.ended) handleNoProgress()
        }

        private fun updateProtectionState() {
            when {
                protectionStatus?.let { it >= PROTECTION_ATTESTATION_REQUIRED } == true -> session.protectionPending = true
                protectionStatus == PROTECTION_ATTESTATION_PENDING -> {
                    session.protectionPending = true
                    session.protectionRetryCount++
                    protectionMaxRetries?.let { session.protectionRetryLimit = it }
                }
                protectionStatus != null -> {
                    session.protectionPending = false
                    session.protectionRetryCount = 0
                    session.protectionRetryLimit = null
                }
            }
        }

        private fun failIfProtectionRequired() {
            val required = protectionStatus?.let { it >= PROTECTION_ATTESTATION_REQUIRED } == true
            val exhausted = session.protectionRetryLimit?.let { session.protectionRetryCount > it } == true
            if (session.protectionPending && (required || newSegmentCount == 0 || exhausted)) {
                throw SabrProtocolException(
                    "SABR stream protection requires attestation " +
                        "(status ${protectionStatus ?: PROTECTION_ATTESTATION_PENDING}, " +
                        "retry=${session.protectionRetryCount}/${session.protectionRetryLimit ?: "unspecified"})",
                    SabrFailureKind.ATTESTATION_REQUIRED,
                )
            }
        }

        private suspend fun handleNoProgress() {
            val playbackPosition = playbackPositionMs?.invoke()?.coerceAtLeast(0L)
            if (playbackPosition != null && session.playerTimeMs - playbackPosition > STARVATION_TOLERANCE_MS) {
                session.requestsWithoutProgress = 0
                awaitPlaybackProgress(playbackPosition, session.maxTimeSinceLastRequestMs)
            } else if (++session.requestsWithoutProgress >= maxRequestsWithoutProgress) {
                throw SabrProtocolException(
                    "SABR made no $label progress for ${session.requestsWithoutProgress} requests " +
                        "(segment=${session.lastSequenceNumber ?: "none"}/${session.initialization?.endSegmentNumber ?: "unknown"}, " +
                        "time=${session.playerTimeMs}/${session.expectedDurationMs()}ms, initialized=${session.initialized}, " +
                        "readahead=${session.minAudioReadaheadMs}..${session.targetAudioReadaheadMs}, " +
                        "maxRequestInterval=${session.maxTimeSinceLastRequestMs}, backoff=${session.backoffTimeMs}, " +
                        "headers=${receivedSequences.joinToString()}/$unselectedHeaderCount)",
                )
            }
        }

        private fun diagnostics() =
            SabrResponseDiagnostics(
                requestNumber = requestNumber,
                requestPlayerTimeMs = requestPlayerTimeMs,
                observedPlaybackPositionMs = observedPosition,
                bufferedRanges = session.bufferedRanges,
                receivedSequences = receivedSequences,
                receivedStartRanges = receivedStartRanges,
                receivedContentLengths = receivedContentLengths,
                initializationReceived = initializationReceived,
                playbackCookieSize = session.playbackCookie?.size ?: 0,
                contextTypes = session.contexts.keys.toSet(),
                activeContextTypes = session.activeContextTypes.toSet(),
                targetAudioReadaheadMs = session.targetAudioReadaheadMs,
                minAudioReadaheadMs = session.minAudioReadaheadMs,
                maxTimeSinceLastRequestMs = session.maxTimeSinceLastRequestMs,
                backoffTimeMs = session.backoffTimeMs,
                responseBytes = responseBytes,
                selectedMediaBytes = newSegmentsBytes,
                cumulativeMediaBytes = session.cumulativeMediaBytes,
                selectedSegmentCount = newSegmentCount,
                cumulativeSegmentCount = session.cumulativeSegmentCount,
                transientRetryCount = session.transientRetryCount,
                noProgressRequestCount = session.requestsWithoutProgress,
                protectionStatus = protectionStatus,
                protectionMaxRetries = protectionMaxRetries,
            )

        private fun logResponse(value: SabrResponseDiagnostics) {
            val notable =
                (value.protectionStatus ?: 0) >= PROTECTION_ATTESTATION_PENDING || value.selectedSegmentCount == 0 ||
                    value.transientRetryCount > 0 || value.noProgressRequestCount > 0 || value.backoffTimeMs > 0L
            val message =
                buildString {
                    append("SABR UMP request #${value.requestNumber}: playerTime=${value.requestPlayerTimeMs}ms")
                    append(", segments=${value.selectedSegmentCount} (total ${value.cumulativeSegmentCount})")
                    append(", mediaBytes=${value.selectedMediaBytes}, responseBytes=${value.responseBytes}")
                    if (value.receivedSequences.isNotEmpty()) append(", sequences=${value.receivedSequences.joinToString()}")
                    value.protectionStatus?.let { append(", protection=$it") }
                    if (value.backoffTimeMs > 0L) append(", backoff=${value.backoffTimeMs}ms")
                    if (value.noProgressRequestCount > 0) append(", noProgressRequests=${value.noProgressRequestCount}")
                }
            if (notable) logger.w(logTag, message, details = value.logDetails()) else logger.d(logTag, message, details = value.logDetails())
        }

        private suspend fun awaitPlaybackProgress(previousPositionMs: Long, maxTimeSinceLastRequestMs: Long) {
            val limit = heartbeatLimit(maxTimeSinceLastRequestMs)
            var waited = 0L
            while (playbackPositionMs?.invoke()?.coerceAtLeast(0L) == previousPositionMs && waited < limit) {
                delay(PLAYBACK_POLL_INTERVAL_MS)
                waited += PLAYBACK_POLL_INTERVAL_MS
            }
        }

        private fun isSelectedHeader(header: SabrMediaHeader): Boolean =
            header.formatId.matches(format) && (header.videoId == null || header.videoId == bootstrap.videoId)

        private fun SabrSegment.endTimeMs(): Long {
            val start = header.startMs
            val duration = header.durationMs
            if (start < 0L || duration < 0L || start > Long.MAX_VALUE - duration) {
                throw SabrProtocolException("SABR segment has an invalid time range")
            }
            return start + duration
        }

        private fun SabrSegment.toChunk() =
            SabrChunk(
                data = data,
                startRange = header.startRange,
                isInitialization = header.isInitSegment,
                sequenceNumber = header.sequenceNumber.takeUnless { header.isInitSegment },
                startMs = header.startMs.takeUnless { header.isInitSegment },
                durationMs = header.durationMs.takeUnless { header.isInitSegment },
            )
    }

    private fun responseDetails(response: HttpResponse): String =
        listOfNotNull(
            response.headers[HttpHeaders.ContentType]?.let { "type=$it" },
            response.headers[HttpHeaders.ContentLength]?.let { "length=$it" },
            response.headers[HttpHeaders.Allow]?.let { "allow=$it" },
            response.headers[HttpHeaders.Server]?.let { "server=$it" },
        ).joinToString()

    private fun retryDetails(status: Int, headers: String, attempts: Int): Map<String, String> =
        buildMap {
            put("http", status.toString())
            put("transientRetry", "$attempts/$MAX_TRANSIENT_RETRIES")
            if (headers.isNotEmpty()) put("responseHeaders", headers)
            putAll(bootstrap.logDetails(format, mediaType))
        }

    private fun retryDelayMs(response: HttpResponse, attempts: Int): Long =
        response.headers[HttpHeaders.RetryAfter]
            ?.toLongOrNull()
            ?.times(1_000L)
            ?.coerceIn(RETRY_BASE_DELAY_MS, RETRY_MAX_DELAY_MS)
            ?: (RETRY_BASE_DELAY_MS shl (attempts - 1))

    private fun requestUrl(url: String, number: Int): String =
        URLBuilder(url).apply { parameters["rn"] = number.toString() }.buildString()

    private fun heartbeatLimit(maxTimeSinceLastRequestMs: Long): Long =
        maxTimeSinceLastRequestMs.takeIf { it > 0L }?.coerceAtMost(MAX_HEARTBEAT_INTERVAL_MS) ?: DEFAULT_HEARTBEAT_INTERVAL_MS

    private fun absoluteDifference(first: Long, second: Long): Long =
        if (first >= second) first - second else second - first

    private fun presence(value: ByteArray?): String = if (value == null) "missing" else "present"

    private fun isSelectedHeader(header: SabrMediaHeader): Boolean =
        header.formatId.matches(format) && (header.videoId == null || header.videoId == bootstrap.videoId)

    private fun SabrBootstrap.logDetails(selectedFormat: SabrFormatId, type: SabrMediaType): Map<String, String> =
        buildMap {
            put("sabrMediaType", type.name)
            put("sabrItag", selectedFormat.itag.toString())
            put("sabrLastModified", selectedFormat.lastModified.toString())
            selectedFormat.xtags?.let { put("sabrXtags", it) }
            put("sabrDurationMs", durationMs.toString())
            contentLengthBytes?.let { put("sabrContentLength", it.toString()) }
            put("sabrMime", mimeType)
            put("sabrClientId", clientName.toString())
            put("sabrClientVersion", clientVersion)
            put("sabrDrc", isDrc.toString())
            put("sabrPoToken", presence(poToken))
            put("sabrUstreamerConfig", "${videoPlaybackUstreamerConfig.size}bytes")
            put("sabrDiscardVideoItag", discardVideoFormat.itag.toString())
            put("sabrDiscardVideoHeight", discardVideoHeight.toString())
            audioTrackId?.takeIf(String::isNotBlank)?.let { put("sabrAudioTrackId", it) }
        }

    private fun SabrResponseDiagnostics.logDetails(): Map<String, String> =
        buildMap {
            put("requestNumber", requestNumber.toString())
            put("playerTimeMs", requestPlayerTimeMs.toString())
            observedPlaybackPositionMs?.let { put("playbackPositionMs", it.toString()) }
            put("segments", selectedSegmentCount.toString())
            put("cumulativeSegments", cumulativeSegmentCount.toString())
            put("responseBytes", responseBytes.toString())
            put("mediaBytes", selectedMediaBytes.toString())
            put("cumulativeMediaBytes", cumulativeMediaBytes.toString())
            put("initializationReceived", initializationReceived.toString())
            put("playbackCookieSize", playbackCookieSize.toString())
            put("targetAudioReadaheadMs", targetAudioReadaheadMs.toString())
            put("minAudioReadaheadMs", minAudioReadaheadMs.toString())
            put("maxTimeSinceLastRequestMs", maxTimeSinceLastRequestMs.toString())
            put("backoffTimeMs", backoffTimeMs.toString())
            put("transientRetryCount", transientRetryCount.toString())
            put("noProgressRequestCount", noProgressRequestCount.toString())
            httpStatus?.let { put("httpStatus", it.toString()) }
            contentType?.let { put("contentType", it) }
            protectionStatus?.let { put("protectionStatus", it.toString()) }
            protectionMaxRetries?.let { put("protectionMaxRetries", it.toString()) }
            if (receivedSequences.isNotEmpty()) put("receivedSequences", receivedSequences.joinToString(","))
        }

    private companion object {
        const val CONTENT_TYPE_PROTOBUF = "application/x-protobuf"
        const val CONTENT_TYPE_UMP = "application/vnd.yt-ump"
        const val KEEP_EXISTING = 2
        const val PROTECTION_ATTESTATION_PENDING = 2
        const val PROTECTION_ATTESTATION_REQUIRED = 3
        const val MAX_REQUEST_COUNT = 10_000
        const val MAX_BACKOFF_MS = 30_000L
        const val MAX_RESPONSE_MEDIA_BYTES = 64L * 1024 * 1024
        const val MAX_CONTEXT_COUNT = 64
        const val MAX_CONTEXT_VALUE_BYTES = 64 * 1024
        const val MAX_CONTEXT_BYTES = 256L * 1024
        const val MAX_PLAYBACK_COOKIE_BYTES = 64 * 1024
        const val MIN_DURATION_TOLERANCE_MS = 2_000L
        const val STARVATION_TOLERANCE_MS = 2_000L
        const val PLAYBACK_POLL_INTERVAL_MS = 250L
        const val DEFAULT_HEARTBEAT_INTERVAL_MS = 60_000L
        const val MAX_HEARTBEAT_INTERVAL_MS = 60_000L
        const val MAX_TRANSIENT_RETRIES = 2
        const val RETRY_BASE_DELAY_MS = 250L
        const val RETRY_MAX_DELAY_MS = 5_000L
        const val READ_BUFFER_SIZE = 64 * 1024
        val TRANSIENT_HTTP_STATUSES = setOf(408, 425, 429, 500, 502, 503, 504)
    }
}

private val SABR_REQUEST_ATTRIBUTE = AttributeKey<Unit>("SabrRequest")

private suspend fun <T> HttpClient.withRedirectsDisabled(block: suspend HttpClient.() -> T): T {
    val rejectRedirect: (HttpResponse) -> Unit = { response ->
        if (response.call.request.attributes.contains(SABR_REQUEST_ATTRIBUTE)) {
            throw SabrProtocolException("SABR HTTP redirects are not allowed")
        }
    }
    val subscription = monitor.subscribe(HttpResponseRedirectEvent, rejectRedirect)
    return try {
        block()
    } finally {
        subscription.dispose()
    }
}
