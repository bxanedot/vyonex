// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

import java.io.ByteArrayOutputStream

private const val MAX_FIELD_NUMBER = 536_870_911L
private const val FIRST_RESERVED_FIELD_NUMBER = 19_000L
private const val LAST_RESERVED_FIELD_NUMBER = 19_999L

private fun isValidFieldNumber(number: Long): Boolean =
    number in 1..MAX_FIELD_NUMBER && number !in FIRST_RESERVED_FIELD_NUMBER..LAST_RESERVED_FIELD_NUMBER

internal class ProtoWriter(initialCapacity: Int = 256) {
    private val output = ByteArrayOutputStream(initialCapacity.coerceAtLeast(1))

    fun int32(field: Int, value: Int) {
        tag(field, WIRE_VARINT)
        writeVarint(value.toLong())
    }

    fun int64(field: Int, value: Long) {
        tag(field, WIRE_VARINT)
        writeVarint(value)
    }

    fun bool(field: Int, value: Boolean) = int32(field, if (value) 1 else 0)

    fun float(field: Int, value: Float) {
        tag(field, WIRE_FIXED32)
        writeFixed32(value.toRawBits())
    }

    fun string(field: Int, value: String) = bytes(field, value.encodeToByteArray())

    fun bytes(field: Int, value: ByteArray) {
        tag(field, WIRE_LENGTH_DELIMITED)
        writeVarint(value.size.toLong())
        output.write(value)
    }

    fun packedInt32(field: Int, values: Collection<Int>) {
        val packed = ProtoWriter()
        values.forEach { packed.writeVarint(it.toLong()) }
        bytes(field, packed.toByteArray())
    }

    fun message(field: Int, block: ProtoWriter.() -> Unit) = bytes(field, ProtoWriter().apply(block).toByteArray())

    fun toByteArray(): ByteArray = output.toByteArray()

    private fun tag(field: Int, wireType: Int) {
        require(isValidFieldNumber(field.toLong())) { "Invalid protobuf field number $field" }
        writeVarint((field.toLong() shl 3) or wireType.toLong())
    }

    private fun writeVarint(input: Long) {
        var value = input
        while (value and -0x80L != 0L) {
            output.write(((value and 0x7fL) or 0x80L).toInt())
            value = value ushr 7
        }
        output.write(value.toInt())
    }

    private fun writeFixed32(value: Int) {
        output.write(value and 0xff)
        output.write((value ushr 8) and 0xff)
        output.write((value ushr 16) and 0xff)
        output.write((value ushr 24) and 0xff)
    }

    private companion object {
        const val WIRE_VARINT = 0
        const val WIRE_LENGTH_DELIMITED = 2
        const val WIRE_FIXED32 = 5
    }
}

internal class ProtoReader(private val data: ByteArray) {
    var position = 0
        private set

    val hasRemaining: Boolean
        get() = position < data.size

    fun tag(): ProtoTag {
        val raw = varint()
        val field = raw ushr 3
        if (!isValidFieldNumber(field)) throw SabrProtocolException("Invalid protobuf field number $field")
        return ProtoTag(field.toInt(), (raw and 7).toInt())
    }

    fun varint(): Long {
        var value = 0L
        var shift = 0
        repeat(MAX_VARINT_BYTES) { index ->
            if (position == data.size) throw SabrProtocolException("Truncated protobuf varint")
            val byte = data[position++].toInt() and 0xff
            if (index == MAX_VARINT_BYTES - 1 && byte > 1) {
                throw SabrProtocolException("Protobuf varint overflows 64 bits")
            }
            value = value or ((byte and 0x7f).toLong() shl shift)
            if (byte and 0x80 == 0) return value
            shift += 7
        }
        throw SabrProtocolException("Protobuf varint is too long")
    }

    fun bool(): Boolean {
        val value = varint()
        if (value < 0) throw SabrProtocolException("Invalid protobuf boolean $value")
        return value != 0L
    }

    fun bytes(): ByteArray {
        val length = varint()
        if (length < 0 || length > Int.MAX_VALUE) throw SabrProtocolException("Invalid protobuf length $length")
        val count = length.toInt()
        if (count > data.size - position) throw SabrProtocolException("Truncated protobuf field")
        return data.copyOfRange(position, position + count).also { position += count }
    }

    fun string(): String = bytes().decodeToString()

    fun skip(tag: ProtoTag) {
        when (tag.wireType) {
            WIRE_VARINT -> varint()
            WIRE_FIXED64 -> advance(8)
            WIRE_LENGTH_DELIMITED -> advance(bytesLength())
            WIRE_FIXED32 -> advance(4)
            else -> throw SabrProtocolException("Unsupported protobuf wire type ${tag.wireType}")
        }
    }

    private fun bytesLength(): Int {
        val length = varint()
        if (length < 0 || length > Int.MAX_VALUE) throw SabrProtocolException("Invalid protobuf length $length")
        return length.toInt()
    }

    private fun advance(count: Int) {
        if (count > data.size - position) throw SabrProtocolException("Truncated protobuf field")
        position += count
    }

    private companion object {
        const val MAX_VARINT_BYTES = 10
        const val WIRE_VARINT = 0
        const val WIRE_FIXED64 = 1
        const val WIRE_LENGTH_DELIMITED = 2
        const val WIRE_FIXED32 = 5
    }
}

internal data class ProtoTag(val field: Int, val wireType: Int)
