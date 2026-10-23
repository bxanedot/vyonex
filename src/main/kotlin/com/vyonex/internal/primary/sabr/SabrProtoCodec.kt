// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

internal object SabrProtoCodec {
    private const val DISCARD_SENTINEL = Int.MAX_VALUE
    private const val MAX_CONTEXT_POLICY_ENTRIES = 256

    fun encodeRequest(
        bootstrap: SabrBootstrap,
        playerTimeMs: Long,
        initialized: Boolean,
        bufferedRanges: List<SabrBufferedRange>,
        playbackCookie: ByteArray?,
        contexts: Collection<SabrContext>,
        activeContextTypes: Set<Int> = emptySet(),
        mediaType: SabrMediaType = SabrMediaType.AUDIO,
    ): ByteArray {
        val videoFormat =
            if (mediaType == SabrMediaType.VIDEO) {
                requireNotNull(bootstrap.selectedVideoFormat) { "SABR video format is not selected" }
            } else {
                bootstrap.discardVideoFormat
            }
        val videoHeight =
            if (mediaType == SabrMediaType.VIDEO) {
                requireNotNull(bootstrap.selectedVideoHeight) { "SABR video height is not selected" }
            } else {
                bootstrap.discardVideoHeight
            }

        return ProtoWriter().apply {
            message(1) {
                if (videoHeight != 0) int32(21, videoHeight)
                if (playerTimeMs != 0L) int64(28, playerTimeMs)
                int32(34, 1)
                float(35, 1f)
                int32(40, if (mediaType == SabrMediaType.AUDIO) 1 else 2)
                if (bootstrap.isDrc) bool(46, true)
                bootstrap.audioTrackId?.takeIf(String::isNotEmpty)?.let { string(69, it) }
            }
            if (initialized) message(2) { formatId(if (mediaType == SabrMediaType.AUDIO) bootstrap.audioFormat else videoFormat) }
            message(2) { formatId(if (mediaType == SabrMediaType.AUDIO) videoFormat else bootstrap.audioFormat) }
            bufferedRanges.forEach { range -> message(3) { bufferedRange(range) } }
            message(3) { discardRange(if (mediaType == SabrMediaType.AUDIO) videoFormat else bootstrap.audioFormat) }
            bytes(5, bootstrap.videoPlaybackUstreamerConfig)
            message(16) { formatId(bootstrap.audioFormat) }
            message(17) { formatId(videoFormat) }
            message(19) {
                message(1) {
                    int32(16, bootstrap.clientName)
                    string(17, bootstrap.clientVersion)
                }
                bootstrap.poToken?.let { bytes(2, it) }
                playbackCookie?.let { bytes(3, it) }
                contexts.filter { it.type in activeContextTypes }.forEach { context ->
                    message(5) {
                        int32(1, context.type)
                        bytes(2, context.value)
                    }
                }
                packedInt32(6, contexts.filterNot { it.type in activeContextTypes }.map(SabrContext::type))
            }
        }.toByteArray()
    }

    fun decodeMediaHeader(data: ByteArray): SabrMediaHeader {
        val reader = ProtoReader(data)
        var headerId = 0
        var videoId: String? = null
        var formatId = SabrFormatId(0)
        var startRange = 0L
        var compression = 0
        var initialization = false
        var sequence = 0
        var startMs = 0L
        var durationMs = 0L
        var contentLength = 0L
        var timeRange: Pair<Long, Long>? = null

        while (reader.hasRemaining) {
            val tag = reader.tag()
            when (tag.field) {
                1 -> { tag.requireWireType(WIRE_VARINT); headerId = reader.intValue(max = 255) }
                2 -> { tag.requireWireType(WIRE_BYTES); videoId = reader.string() }
                3 -> { tag.requireWireType(WIRE_VARINT); formatId = formatId.copy(itag = reader.intValue()) }
                4 -> { tag.requireWireType(WIRE_VARINT); formatId = formatId.copy(lastModified = reader.unsignedValue()) }
                5 -> { tag.requireWireType(WIRE_BYTES); formatId = formatId.copy(xtags = reader.string()) }
                6 -> { tag.requireWireType(WIRE_VARINT); startRange = reader.unsignedValue() }
                7 -> { tag.requireWireType(WIRE_VARINT); compression = reader.intValue() }
                8 -> { tag.requireWireType(WIRE_VARINT); initialization = reader.bool() }
                9 -> { tag.requireWireType(WIRE_VARINT); sequence = reader.intValue() }
                11 -> { tag.requireWireType(WIRE_VARINT); startMs = reader.unsignedValue() }
                12 -> { tag.requireWireType(WIRE_VARINT); durationMs = reader.unsignedValue() }
                13 -> { tag.requireWireType(WIRE_BYTES); formatId = decodeFormatId(reader.bytes()) }
                14 -> { tag.requireWireType(WIRE_VARINT); contentLength = reader.unsignedValue() }
                15 -> { tag.requireWireType(WIRE_BYTES); timeRange = decodeTimeRange(reader.bytes()) }
                else -> reader.skip(tag)
            }
        }

        if (headerId !in 0..255) throw SabrProtocolException("Invalid SABR media header ID $headerId")
        if (formatId.itag <= 0) throw SabrProtocolException("SABR media header has no format ID")
        if (startRange < 0 || contentLength < 0 || startMs < 0 || durationMs < 0) {
            throw SabrProtocolException("SABR media header contains an overflowing unsigned value")
        }
        if (startMs == 0L) startMs = timeRange?.first ?: 0L
        if (durationMs == 0L) durationMs = timeRange?.second ?: 0L
        return SabrMediaHeader(
            headerId = headerId,
            videoId = videoId,
            formatId = formatId,
            startRange = startRange,
            compressionAlgorithm = compression,
            isInitSegment = initialization,
            sequenceNumber = sequence,
            startMs = startMs,
            durationMs = durationMs,
            contentLength = contentLength,
        )
    }

    fun decodeFormatInitialization(data: ByteArray): SabrFormatInitialization? {
        val reader = ProtoReader(data)
        var formatId: SabrFormatId? = null
        var endTimeMs: Long? = null
        var endSegmentNumber: Int? = null
        var durationUnits: Long? = null
        var durationTimescale: Long? = null
        while (reader.hasRemaining) {
            val tag = reader.tag()
            when (tag.field) {
                2 -> { tag.requireWireType(WIRE_BYTES); formatId = decodeFormatId(reader.bytes()) }
                3 -> { tag.requireWireType(WIRE_VARINT); endTimeMs = reader.unsignedValue() }
                4 -> { tag.requireWireType(WIRE_VARINT); endSegmentNumber = reader.intValue() }
                9 -> { tag.requireWireType(WIRE_VARINT); durationUnits = reader.unsignedValue() }
                10 -> { tag.requireWireType(WIRE_VARINT); durationTimescale = reader.intValue(min = 1).toLong() }
                else -> reader.skip(tag)
            }
        }
        val id = formatId ?: return null
        val durationMs =
            if (durationUnits != null && durationTimescale != null) {
                ticksToMs(durationUnits, durationTimescale)
            } else {
                endTimeMs
            }
        return SabrFormatInitialization(id, endTimeMs, endSegmentNumber, durationMs)
    }

    fun decodeNextRequestPolicy(data: ByteArray): SabrEvent.NextRequestPolicy {
        val reader = ProtoReader(data)
        var targetAudioReadaheadMs = 0L
        var maxTimeSinceLastRequestMs = 0L
        var backoffTimeMs = 0L
        var minAudioReadaheadMs = 0L
        var playbackCookie: ByteArray? = null
        while (reader.hasRemaining) {
            val tag = reader.tag()
            when (tag.field) {
                1 -> { tag.requireWireType(WIRE_VARINT); targetAudioReadaheadMs = reader.unsignedValue() }
                3 -> { tag.requireWireType(WIRE_VARINT); maxTimeSinceLastRequestMs = reader.unsignedValue() }
                4 -> { tag.requireWireType(WIRE_VARINT); backoffTimeMs = reader.unsignedValue() }
                5 -> { tag.requireWireType(WIRE_VARINT); minAudioReadaheadMs = reader.unsignedValue() }
                7 -> { tag.requireWireType(WIRE_BYTES); playbackCookie = reader.bytes() }
                else -> reader.skip(tag)
            }
        }
        if (targetAudioReadaheadMs < 0 || maxTimeSinceLastRequestMs < 0 || backoffTimeMs < 0 || minAudioReadaheadMs < 0) {
            throw SabrProtocolException("SABR next-request policy contains an overflowing unsigned value")
        }
        return SabrEvent.NextRequestPolicy(
            targetAudioReadaheadMs,
            maxTimeSinceLastRequestMs,
            backoffTimeMs,
            minAudioReadaheadMs,
            playbackCookie,
        )
    }

    fun decodeRedirect(data: ByteArray): String? {
        val reader = ProtoReader(data)
        while (reader.hasRemaining) {
            val tag = reader.tag()
            if (tag.field == 1) {
                tag.requireWireType(WIRE_BYTES)
                return reader.string()
            }
            reader.skip(tag)
        }
        return null
    }

    fun decodeError(data: ByteArray): SabrEvent.Error {
        val reader = ProtoReader(data)
        var type: String? = null
        var code = 0
        while (reader.hasRemaining) {
            val tag = reader.tag()
            when (tag.field) {
                1 -> { tag.requireWireType(WIRE_BYTES); type = reader.string() }
                2 -> { tag.requireWireType(WIRE_VARINT); code = reader.intValue() }
                else -> reader.skip(tag)
            }
        }
        return SabrEvent.Error(type, code)
    }

    fun decodeContextUpdate(data: ByteArray): SabrContext? {
        val reader = ProtoReader(data)
        var type = 0
        var value: ByteArray? = null
        var sendByDefault = false
        var writePolicy = 0
        while (reader.hasRemaining) {
            val tag = reader.tag()
            when (tag.field) {
                1 -> { tag.requireWireType(WIRE_VARINT); type = reader.intValue() }
                3 -> { tag.requireWireType(WIRE_BYTES); value = reader.bytes() }
                4 -> { tag.requireWireType(WIRE_VARINT); sendByDefault = reader.bool() }
                5 -> { tag.requireWireType(WIRE_VARINT); writePolicy = reader.intValue() }
                else -> reader.skip(tag)
            }
        }
        return value?.let { SabrContext(type, it, sendByDefault, writePolicy) }
    }

    fun decodeStreamProtectionStatus(data: ByteArray): SabrEvent.StreamProtectionStatus {
        val reader = ProtoReader(data)
        var status = 0
        var maxRetries = 0
        while (reader.hasRemaining) {
            val tag = reader.tag()
            when (tag.field) {
                1 -> { tag.requireWireType(WIRE_VARINT); status = reader.intValue() }
                2 -> { tag.requireWireType(WIRE_VARINT); maxRetries = reader.intValue() }
                else -> reader.skip(tag)
            }
        }
        return SabrEvent.StreamProtectionStatus(status, maxRetries)
    }

    fun decodeContextSendingPolicy(data: ByteArray): SabrEvent.ContextSendingPolicy {
        val reader = ProtoReader(data)
        val start = mutableSetOf<Int>()
        val stop = mutableSetOf<Int>()
        val discard = mutableSetOf<Int>()
        var entryCount = 0
        while (reader.hasRemaining) {
            val tag = reader.tag()
            val destination = when (tag.field) {
                1 -> start
                2 -> stop
                3 -> discard
                else -> null
            }
            if (destination == null) {
                reader.skip(tag)
            } else if (tag.wireType == WIRE_BYTES) {
                val packed = ProtoReader(reader.bytes())
                while (packed.hasRemaining) {
                    if (++entryCount > MAX_CONTEXT_POLICY_ENTRIES) throw SabrProtocolException("SABR context policy exceeded the entry limit")
                    destination += packed.intValue()
                }
            } else {
                tag.requireWireType(WIRE_VARINT)
                if (++entryCount > MAX_CONTEXT_POLICY_ENTRIES) throw SabrProtocolException("SABR context policy exceeded the entry limit")
                destination += reader.intValue()
            }
        }
        return SabrEvent.ContextSendingPolicy(start, stop, discard)
    }

    private fun decodeTimeRange(data: ByteArray): Pair<Long, Long>? {
        val reader = ProtoReader(data)
        var startTicks = 0L
        var durationTicks = 0L
        var timescale = 0L
        while (reader.hasRemaining) {
            val tag = reader.tag()
            when (tag.field) {
                1 -> { tag.requireWireType(WIRE_VARINT); startTicks = reader.unsignedValue() }
                2 -> { tag.requireWireType(WIRE_VARINT); durationTicks = reader.unsignedValue() }
                3 -> { tag.requireWireType(WIRE_VARINT); timescale = reader.intValue(min = 1).toLong() }
                else -> reader.skip(tag)
            }
        }
        if (startTicks < 0 || durationTicks < 0 || timescale !in 1..Int.MAX_VALUE.toLong()) return null
        return ticksToMs(startTicks, timescale) to ticksToMs(durationTicks, timescale)
    }

    private fun ticksToMs(ticks: Long, timescale: Long): Long {
        val seconds = ticks / timescale
        if (seconds > Long.MAX_VALUE / 1_000L) throw SabrProtocolException("SABR time range exceeds the supported duration")
        return seconds * 1_000L + ticks % timescale * 1_000L / timescale
    }

    private fun ProtoWriter.formatId(format: SabrFormatId) {
        int32(1, format.itag)
        if (format.lastModified != 0L) int64(2, format.lastModified)
        format.xtags?.takeIf(String::isNotEmpty)?.let { string(3, it) }
    }

    private fun ProtoWriter.bufferedRange(range: SabrBufferedRange) {
        message(1) { formatId(range.formatId) }
        if (range.startTimeMs != 0L) int64(2, range.startTimeMs)
        int64(3, range.durationMs)
        int32(4, range.startSegmentIndex)
        int32(5, range.endSegmentIndex)
        message(6) {
            if (range.startTimeMs != 0L) int64(1, range.startTimeMs)
            int64(2, range.durationMs)
            int32(3, 1_000)
        }
    }

    private fun ProtoWriter.discardRange(format: SabrFormatId) {
        message(1) { formatId(format) }
        int64(3, DISCARD_SENTINEL.toLong())
        int32(4, DISCARD_SENTINEL)
        int32(5, DISCARD_SENTINEL)
        message(6) {
            int64(2, DISCARD_SENTINEL.toLong())
            int32(3, 1_000)
        }
    }

    private fun decodeFormatId(data: ByteArray): SabrFormatId {
        val reader = ProtoReader(data)
        var itag = 0
        var lastModified = 0L
        var xtags: String? = null
        while (reader.hasRemaining) {
            val tag = reader.tag()
            when (tag.field) {
                1 -> { tag.requireWireType(WIRE_VARINT); itag = reader.intValue() }
                2 -> { tag.requireWireType(WIRE_VARINT); lastModified = reader.unsignedValue() }
                3 -> { tag.requireWireType(WIRE_BYTES); xtags = reader.string() }
                else -> reader.skip(tag)
            }
        }
        return SabrFormatId(itag, lastModified, xtags)
    }

    private fun ProtoTag.requireWireType(expected: Int) {
        if (wireType != expected) throw SabrProtocolException("Unexpected protobuf wire type $wireType for field $field")
    }

    private fun ProtoReader.unsignedValue(): Long {
        val value = varint()
        if (value < 0) throw SabrProtocolException("Protobuf unsigned value exceeds Long")
        return value
    }

    private fun ProtoReader.intValue(max: Int = Int.MAX_VALUE, min: Int = 0): Int {
        val value = unsignedValue()
        if (value !in min.toLong()..max.toLong()) throw SabrProtocolException("Protobuf unsigned value $value does not fit in Int")
        return value.toInt()
    }

    private const val WIRE_VARINT = 0
    private const val WIRE_BYTES = 2
}
