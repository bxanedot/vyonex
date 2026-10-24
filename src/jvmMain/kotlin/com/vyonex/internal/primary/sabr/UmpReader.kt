// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

internal data class UmpPart(val type: Int, val data: ByteArray) {
    override fun equals(other: Any?): Boolean = other is UmpPart && type == other.type && data.contentEquals(other.data)

    override fun hashCode(): Int = 31 * type + data.contentHashCode()
}

/** Incremental reader for YouTube's length-delimited UMP parts. */
internal class UmpReader(private val maxPartSize: Int = DEFAULT_MAX_PART_SIZE) {
    private var buffer = ByteArray(0)
    private var firstPendingByte = 0
    private var lastByte = 0
    private val pendingSize: Int
        get() = lastByte - firstPendingByte

    init {
        require(maxPartSize >= 0) { "Maximum UMP part size must be non-negative" }
    }

    fun feed(chunk: ByteArray): List<UmpPart> = feed(chunk, 0, chunk.size)

    internal fun feed(chunk: ByteArray, offset: Int, length: Int): List<UmpPart> {
        require(offset >= 0 && length >= 0 && offset <= chunk.size - length) { "Invalid UMP input range" }
        if (pendingSize.toLong() + length > MAX_PENDING_BYTES) {
            throw SabrProtocolException("Incomplete UMP part exceeded the pending byte limit")
        }
        append(chunk, offset, length)
        if (pendingSize == 0) return emptyList()

        val parts = mutableListOf<UmpPart>()
        var cursor = firstPendingByte
        while (cursor < lastByte) {
            val type = readInteger(cursor) ?: break
            val size = readInteger(type.nextByte) ?: break
            if (size.value > maxPartSize) throw SabrProtocolException("UMP part is too large: ${size.value} bytes")
            val partEnd = size.nextByte.toLong() + size.value
            if (partEnd > lastByte) break
            parts += UmpPart(type.value, buffer.copyOfRange(size.nextByte, partEnd.toInt()))
            cursor = partEnd.toInt()
        }

        firstPendingByte = cursor
        if (firstPendingByte == lastByte) {
            firstPendingByte = 0
            lastByte = 0
        }
        return parts
    }

    fun finish() {
        if (pendingSize > 0) throw SabrProtocolException("Truncated UMP response ($pendingSize bytes remain)")
    }

    private fun append(chunk: ByteArray, offset: Int, length: Int) {
        if (length == 0) return
        makeRoom(length)
        chunk.copyInto(buffer, lastByte, offset, offset + length)
        lastByte += length
    }

    private fun makeRoom(additional: Int) {
        if (buffer.size - lastByte >= additional) return
        val needed = pendingSize.toLong() + additional
        if (needed > MAX_PENDING_BYTES) throw SabrProtocolException("Incomplete UMP part exceeded the pending byte limit")
        if (firstPendingByte > 0 && buffer.size.toLong() >= needed) {
            buffer.copyInto(buffer, 0, firstPendingByte, lastByte)
            lastByte = pendingSize
            firstPendingByte = 0
            return
        }

        val doubled = buffer.size.coerceAtLeast(DEFAULT_BUFFER_SIZE).toLong() * 2
        val capacity = maxOf(needed, doubled).coerceAtMost(MAX_PENDING_BYTES).toInt()
        val expanded = ByteArray(capacity)
        buffer.copyInto(expanded, 0, firstPendingByte, lastByte)
        lastByte = pendingSize
        firstPendingByte = 0
        buffer = expanded
    }

    private fun readInteger(offset: Int): UmpInteger? {
        if (offset >= lastByte) return null
        val marker = buffer[offset].toInt() and 0xff
        val byteCount = when {
            marker < 0x80 -> 1
            marker < 0xc0 -> 2
            marker < 0xe0 -> 3
            marker < 0xf0 -> 4
            else -> 5
        }
        if (lastByte - offset < byteCount) return null

        fun byte(index: Int): Long = (buffer[offset + index].toInt() and 0xff).toLong()

        val value =
            when (byteCount) {
                1 -> marker.toLong()
                2 -> (marker and 0x3f).toLong() + 64L * byte(1)
                3 -> (marker and 0x1f).toLong() + 32L * (byte(1) + 256L * byte(2))
                4 -> (marker and 0x0f).toLong() + 16L * (byte(1) + 256L * (byte(2) + 256L * byte(3)))
                else -> byte(1) + 256L * (byte(2) + 256L * (byte(3) + 256L * byte(4)))
            }
        if (value > Int.MAX_VALUE) throw SabrProtocolException("UMP varint exceeds Int.MAX_VALUE")
        return UmpInteger(value.toInt(), offset + byteCount)
    }

    private data class UmpInteger(val value: Int, val nextByte: Int)

    companion object {
        const val DEFAULT_MAX_PART_SIZE = 8 * 1024 * 1024
        private const val DEFAULT_BUFFER_SIZE = 8 * 1024
        private const val MAX_PENDING_BYTES = 16L * 1024 * 1024
    }
}

internal object UmpPartType {
    const val MEDIA_HEADER = 20
    const val MEDIA = 21
    const val MEDIA_END = 22
    const val NEXT_REQUEST_POLICY = 35
    const val FORMAT_INITIALIZATION_METADATA = 42
    const val SABR_REDIRECT = 43
    const val SABR_ERROR = 44
    const val RELOAD_PLAYER_RESPONSE = 46
    const val SABR_CONTEXT_UPDATE = 57
    const val STREAM_PROTECTION_STATUS = 58
    const val SABR_CONTEXT_SENDING_POLICY = 59
    const val END_OF_TRACK = 62
}
