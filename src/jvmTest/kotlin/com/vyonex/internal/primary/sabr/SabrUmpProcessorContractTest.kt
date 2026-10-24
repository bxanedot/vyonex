// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SabrUmpProcessorContractTest {
    @Test
    fun reassemblesSplitMediaPartsAndEmitsACompleteSegment() {
        val header = SabrMediaHeader(headerId = 4, formatId = SabrFormatId(140, 1234))
        val processor = SabrUmpProcessor()

        assertEquals(listOf(SabrEvent.MediaHeader(header)), processor.feed(part(UmpPartType.MEDIA_HEADER, headerBytes(header))))
        assertEquals(emptyList<SabrEvent>(), processor.feed(part(UmpPartType.MEDIA, byteArrayOf(4, 1, 2))))
        assertEquals(emptyList<SabrEvent>(), processor.feed(part(UmpPartType.MEDIA, byteArrayOf(4, 3))))
        assertEquals(
            listOf(SabrEvent.Segment(SabrSegment(header, byteArrayOf(1, 2, 3)))),
            processor.feed(part(UmpPartType.MEDIA_END, byteArrayOf(4))),
        )
        processor.finish()
    }

    @Test
    fun validatesFixedLengthAndDoesNotBufferUnselectedMedia() {
        val fixedHeader = SabrMediaHeader(headerId = 1, formatId = SabrFormatId(140, 7), contentLength = 2)
        val fixed = SabrUmpProcessor()
        fixed.feed(part(UmpPartType.MEDIA_HEADER, headerBytes(fixedHeader)))
        fixed.feed(part(UmpPartType.MEDIA, byteArrayOf(1, 9, 8)))
        assertEquals(
            listOf(SabrEvent.Segment(SabrSegment(fixedHeader, byteArrayOf(9, 8)))),
            fixed.feed(part(UmpPartType.MEDIA_END, byteArrayOf(1))),
        )
        fixed.finish()

        val ignoredHeader = fixedHeader.copy(headerId = 2, compressionAlgorithm = 1, contentLength = 1)
        val ignored = SabrUmpProcessor(shouldCollectMedia = { false }, maxCollectedSegmentSize = 0, maxPendingMediaBytes = 0)
        assertEquals(listOf(SabrEvent.MediaHeader(ignoredHeader)), ignored.feed(part(UmpPartType.MEDIA_HEADER, headerBytes(ignoredHeader))))
        assertEquals(emptyList<SabrEvent>(), ignored.feed(part(UmpPartType.MEDIA, byteArrayOf(2, 4))))
        assertEquals(emptyList<SabrEvent>(), ignored.feed(part(UmpPartType.MEDIA_END, byteArrayOf(2))))
        ignored.finish()
    }

    @Test
    fun rejectsDuplicateUnknownIncompleteAndOversizedSegments() {
        val header = SabrMediaHeader(headerId = 3, formatId = SabrFormatId(140, 4), contentLength = 3)
        val encodedHeader = part(UmpPartType.MEDIA_HEADER, headerBytes(header))
        val duplicate = SabrUmpProcessor()
        duplicate.feed(encodedHeader)
        assertThrows(SabrProtocolException::class.java) { duplicate.feed(encodedHeader) }

        val unknown = SabrUmpProcessor()
        assertThrows(SabrProtocolException::class.java) { unknown.feed(part(UmpPartType.MEDIA, byteArrayOf(3, 1))) }

        val incomplete = SabrUmpProcessor()
        incomplete.feed(encodedHeader)
        assertThrows(SabrProtocolException::class.java) { incomplete.finish() }

        val tooLarge = SabrUmpProcessor(maxCollectedSegmentSize = 2)
        assertThrows(SabrProtocolException::class.java) { tooLarge.feed(encodedHeader) }
    }

    @Test
    fun enforcesTotalResponseByteLimit() {
        val processor = SabrUmpProcessor(maxResponseBytes = 1)
        assertThrows(SabrProtocolException::class.java) {
            processor.feed(part(UmpPartType.RELOAD_PLAYER_RESPONSE, byteArrayOf()))
        }
    }

    private fun headerBytes(header: SabrMediaHeader): ByteArray =
        ProtoWriter().apply {
            int32(1, header.headerId)
            int32(3, header.formatId.itag)
            int64(4, header.formatId.lastModified)
            int32(7, header.compressionAlgorithm)
            int64(14, header.contentLength)
        }.toByteArray()

    private fun part(type: Int, data: ByteArray): ByteArray {
        require(type in 0..127 && data.size in 0..127)
        return byteArrayOf(type.toByte(), data.size.toByte()) + data
    }
}
