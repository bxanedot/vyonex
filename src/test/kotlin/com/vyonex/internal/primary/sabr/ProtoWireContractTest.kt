// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ProtoWireContractTest {
    @Test
    fun writerEmitsStandardWireEncodings() {
        val encoded =
            ProtoWriter(initialCapacity = 1)
                .apply {
                    int32(1, 150)
                    int64(2, 300)
                    bool(3, true)
                    float(4, 1.5f)
                    string(5, "x")
                    bytes(6, byteArrayOf(0, -1))
                    message(7) { int32(1, 1) }
                    packedInt32(8, listOf(1, 2))
                }.toByteArray()

        assertArrayEquals(
            byteArrayOf(
                0x08, 0x96.toByte(), 0x01, 0x10, 0xac.toByte(), 0x02, 0x18, 0x01,
                0x25, 0x00, 0x00, 0xc0.toByte(), 0x3f, 0x2a, 0x01, 0x78,
                0x32, 0x02, 0x00, 0xff.toByte(), 0x3a, 0x02, 0x08, 0x01, 0x42, 0x02, 0x01, 0x02,
            ),
            encoded,
        )
    }

    @Test
    fun readerConsumesKnownValuesAndSkipsUnknownWireTypes() {
        val reader = ProtoReader(byteArrayOf(0x08, 0x96.toByte(), 0x01, 0x09, 1, 2, 3, 4, 5, 6, 7, 8, 0x15, 9, 10, 11, 12))

        assertEquals(ProtoTag(field = 1, wireType = 0), reader.tag())
        assertEquals(150L, reader.varint())
        val fixed64 = reader.tag()
        assertEquals(ProtoTag(field = 1, wireType = 1), fixed64)
        reader.skip(fixed64)
        val fixed32 = reader.tag()
        assertEquals(ProtoTag(field = 2, wireType = 5), fixed32)
        reader.skip(fixed32)
        assertFalse(reader.hasRemaining)
    }

    @Test
    fun rejectsInvalidTagsAndTruncatedValues() {
        val boolReader = ProtoReader(byteArrayOf(0x08, 0x00))

        assertThrows(SabrProtocolException::class.java) { ProtoReader(byteArrayOf(0)).tag() }
        assertThrows(SabrProtocolException::class.java) { ProtoReader(varint(19_000L shl 3)).tag() }
        assertThrows(SabrProtocolException::class.java) { ProtoReader(byteArrayOf(0x80.toByte())).varint() }
        assertThrows(SabrProtocolException::class.java) { ProtoReader(byteArrayOf(0x0a, 0x02, 0x01)).apply { tag(); bytes() } }
        assertThrows(SabrProtocolException::class.java) { ProtoReader(byteArrayOf(0x0b)).apply { skip(tag()) } }
        boolReader.tag()
        assertFalse(boolReader.bool())
        assertFalse(boolReader.hasRemaining)
    }

    private fun varint(input: Long): ByteArray {
        var value = input
        val bytes = mutableListOf<Byte>()
        while (value and -0x80L != 0L) {
            bytes += ((value and 0x7fL) or 0x80L).toByte()
            value = value ushr 7
        }
        bytes += value.toByte()
        return bytes.toByteArray()
    }
}
