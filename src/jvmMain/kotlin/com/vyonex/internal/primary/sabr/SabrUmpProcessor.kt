// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

/** Turns UMP parts into session events and completed media segments. */
internal class SabrUmpProcessor(
    maxPartSize: Int = UmpReader.DEFAULT_MAX_PART_SIZE,
    private val shouldCollectMedia: (SabrMediaHeader) -> Boolean = { true },
    private val maxCollectedSegmentSize: Int = DEFAULT_MAX_COLLECTED_SEGMENT_SIZE,
    private val maxPendingMediaBytes: Int = DEFAULT_MAX_PENDING_MEDIA_BYTES,
    private val maxResponseBytes: Long = DEFAULT_MAX_RESPONSE_BYTES,
) {
    private val reader = UmpReader(maxPartSize)
    private val mediaByHeader = mutableMapOf<Int, PendingMedia>()
    private var pendingBytes = 0L
    private var responseBytes = 0L

    fun feed(chunk: ByteArray): List<SabrEvent> = feed(chunk, chunk.size)

    internal fun feed(chunk: ByteArray, length: Int): List<SabrEvent> {
        accountResponseSize(length)
        return reader.feed(chunk, 0, length).flatMap(::handlePart)
    }

    fun finish() {
        reader.finish()
        if (mediaByHeader.isNotEmpty()) {
            val ids = mediaByHeader.keys.sorted().joinToString()
            throw SabrProtocolException("SABR response ended with incomplete media headers: $ids")
        }
    }

    private fun accountResponseSize(count: Int) {
        require(count >= 0) { "Response byte count must not be negative" }
        if (count.toLong() > maxResponseBytes - responseBytes) {
            throw SabrProtocolException("SABR response exceeded $maxResponseBytes bytes")
        }
        responseBytes += count
    }

    private fun handlePart(part: UmpPart): List<SabrEvent> =
        when (part.type) {
            UmpPartType.MEDIA_HEADER -> listOf(beginMedia(part.data))
            UmpPartType.MEDIA -> {
                appendMedia(part.data)
                emptyList()
            }
            UmpPartType.MEDIA_END -> listOfNotNull(completeMedia(part.data))
            UmpPartType.FORMAT_INITIALIZATION_METADATA -> listOf(SabrEvent.FormatInitialized(SabrProtoCodec.decodeFormatInitialization(part.data)))
            UmpPartType.NEXT_REQUEST_POLICY -> listOf(SabrProtoCodec.decodeNextRequestPolicy(part.data))
            UmpPartType.SABR_REDIRECT -> SabrProtoCodec.decodeRedirect(part.data)?.let { listOf(SabrEvent.Redirect(it)) }.orEmpty()
            UmpPartType.SABR_ERROR -> listOf(SabrProtoCodec.decodeError(part.data))
            UmpPartType.RELOAD_PLAYER_RESPONSE -> listOf(SabrEvent.ReloadPlayerResponse)
            UmpPartType.SABR_CONTEXT_UPDATE -> SabrProtoCodec.decodeContextUpdate(part.data)?.let { listOf(SabrEvent.ContextUpdate(it)) }.orEmpty()
            UmpPartType.STREAM_PROTECTION_STATUS -> listOf(SabrProtoCodec.decodeStreamProtectionStatus(part.data))
            UmpPartType.SABR_CONTEXT_SENDING_POLICY -> listOf(SabrProtoCodec.decodeContextSendingPolicy(part.data))
            UmpPartType.END_OF_TRACK -> listOf(SabrEvent.EndOfTrack)
            else -> emptyList()
        }

    private fun beginMedia(data: ByteArray): SabrEvent.MediaHeader {
        val header = SabrProtoCodec.decodeMediaHeader(data)
        if (mediaByHeader.containsKey(header.headerId)) {
            throw SabrProtocolException("Duplicate SABR media header ID ${header.headerId}")
        }
        val collect = shouldCollectMedia(header)
        if (collect && header.compressionAlgorithm != NO_COMPRESSION) {
            throw SabrProtocolException("Unsupported compression ${header.compressionAlgorithm} for SABR media segment")
        }
        if (collect && header.contentLength > maxCollectedSegmentSize) {
            throw SabrProtocolException("SABR segment is too large: ${header.contentLength} bytes")
        }
        mediaByHeader[header.headerId] = PendingMedia(header, collect)
        return SabrEvent.MediaHeader(header)
    }

    private fun appendMedia(data: ByteArray) {
        if (data.isEmpty()) throw SabrProtocolException("Empty UMP MEDIA part")
        val headerId = data[0].toInt() and 0xff
        val pending = mediaByHeader[headerId] ?: throw SabrProtocolException("UMP MEDIA refers to unknown header ID $headerId")
        val payloadBytes = data.size - 1
        val destinationOffset = pending.byteCount.toInt()
        pending.byteCount += payloadBytes
        if (pending.header.contentLength > 0 && pending.byteCount > pending.header.contentLength) {
            throw SabrProtocolException("SABR segment ${pending.header.sequenceNumber} exceeded its content length")
        }
        if (!pending.collect || payloadBytes == 0) return
        if (pending.byteCount > maxCollectedSegmentSize) throw SabrProtocolException("SABR segment is too large: ${pending.byteCount} bytes")

        if (pending.header.contentLength > 0) {
            val target = pending.bytes ?: ByteArray(pending.header.contentLength.toInt()).also { bytes ->
                reserve(pending, bytes.size)
                pending.bytes = bytes
            }
            data.copyInto(target, destinationOffset, 1)
        } else {
            reserve(pending, payloadBytes)
            pending.chunks += data.copyOfRange(1, data.size)
        }
    }

    private fun reserve(pending: PendingMedia, count: Int) {
        if (count > maxPendingMediaBytes - pendingBytes) {
            throw SabrProtocolException("SABR pending media exceeded $maxPendingMediaBytes bytes")
        }
        pendingBytes += count
        pending.reservedBytes += count
    }

    private fun completeMedia(data: ByteArray): SabrEvent.Segment? {
        if (data.isEmpty()) throw SabrProtocolException("Empty UMP MEDIA_END part")
        val headerId = data[0].toInt() and 0xff
        val pending = mediaByHeader.remove(headerId) ?: throw SabrProtocolException("UMP MEDIA_END refers to unknown header ID $headerId")
        if (pending.header.contentLength > 0 && pending.byteCount != pending.header.contentLength) {
            throw SabrProtocolException(
                "SABR segment ${pending.header.sequenceNumber} has ${pending.byteCount} bytes, " +
                    "expected ${pending.header.contentLength}",
            )
        }
        if (!pending.collect) return null
        pendingBytes -= pending.reservedBytes
        val payload = pending.bytes ?: ByteArray(pending.byteCount.toInt()).also { output ->
            var offset = 0
            pending.chunks.forEach { chunk ->
                chunk.copyInto(output, offset)
                offset += chunk.size
            }
        }
        return SabrEvent.Segment(SabrSegment(pending.header, payload))
    }

    private data class PendingMedia(
        val header: SabrMediaHeader,
        val collect: Boolean,
        val chunks: MutableList<ByteArray> = mutableListOf(),
        var bytes: ByteArray? = null,
        var byteCount: Long = 0,
        var reservedBytes: Long = 0,
    )

    private companion object {
        const val NO_COMPRESSION = 0
        const val DEFAULT_MAX_COLLECTED_SEGMENT_SIZE = 16 * 1024 * 1024
        const val DEFAULT_MAX_PENDING_MEDIA_BYTES = 32 * 1024 * 1024
        const val DEFAULT_MAX_RESPONSE_BYTES = 96L * 1024 * 1024
    }
}
