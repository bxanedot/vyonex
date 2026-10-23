// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UmpReaderContractTest {
    @Test
    fun readsPartsSplitAcrossChunksAndFinishesCleanly() {
        val reader = UmpReader()
        val part = frame(20, byteArrayOf(1, 2, 3))

        assertEquals(emptyList<UmpPart>(), reader.feed(part.copyOfRange(0, 1)))
        assertEquals(emptyList<UmpPart>(), reader.feed(part.copyOfRange(1, 3)))
        assertEquals(listOf(UmpPart(20, byteArrayOf(1, 2, 3))), reader.feed(part.copyOfRange(3, part.size)))
        reader.finish()
    }

    @Test
    fun readsExtendedTypeAndPayloadLengthAndRespectsInputSlice() {
        val payload = ByteArray(130) { it.toByte() }
        val first = frame(130, payload)
        val second = frame(21, byteArrayOf(9))
        val input = byteArrayOf(99) + first + second + byteArrayOf(88)
        val reader = UmpReader()

        assertEquals(
            listOf(UmpPart(130, payload), UmpPart(21, byteArrayOf(9))),
            reader.feed(input, 1, input.size - 2),
        )
        reader.finish()
    }

    @Test
    fun rejectsOversizedAndTruncatedPartsAndInvalidRanges() {
        assertThrows(SabrProtocolException::class.java) { UmpReader(maxPartSize = 2).feed(byteArrayOf(1, 3)) }
        assertThrows(SabrProtocolException::class.java) { UmpReader().feed(byteArrayOf(0xf0.toByte(), 0, 0, 0, 0x80.toByte(), 0)) }
        assertThrows(SabrProtocolException::class.java) { UmpReader().apply { feed(byteArrayOf(20, 3, 1)); finish() } }
        assertThrows(IllegalArgumentException::class.java) { UmpReader().feed(byteArrayOf(1), 1, 1) }
        assertThrows(IllegalArgumentException::class.java) { UmpReader(maxPartSize = -1) }
    }

    private fun frame(type: Int, payload: ByteArray): ByteArray = encode(type) + encode(payload.size) + payload

    private fun encode(value: Int): ByteArray =
        when {
            value < 0x80 -> byteArrayOf(value.toByte())
            value < 0x4000 -> byteArrayOf((0x80 or (value and 0x3f)).toByte(), (value ushr 6).toByte())
            else -> error("Contract test only encodes values that fit in two UMP bytes")
        }
}
