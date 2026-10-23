// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SabrModelsContractTest {
    @Test
    fun bootstrapAndSegmentCompareByteArraysByContents() {
        val first = bootstrap()
        val equal = bootstrap()
        val otherToken = bootstrap(poToken = byteArrayOf(8, 9))

        assertEquals(first, equal)
        assertEquals(first.hashCode(), equal.hashCode())
        assertFalse(first == otherToken)

        val header = SabrMediaHeader(headerId = 2, formatId = SabrFormatId(140))
        assertEquals(SabrSegment(header, byteArrayOf(1, 2)), SabrSegment(header, byteArrayOf(1, 2)))
    }

    @Test
    fun chunkRangeIsExclusiveAndRejectsOverflow() {
        val chunk = SabrChunk(byteArrayOf(1, 2), startRange = 10, isInitialization = false, sequenceNumber = 3, startMs = null, durationMs = null)

        assertEquals(12, chunk.endRangeExclusive)
        assertThrows(IllegalArgumentException::class.java) {
            SabrChunk(byteArrayOf(1), Long.MAX_VALUE, false, null, null, null).endRangeExclusive
        }
    }

    @Test
    fun recoveryRetriesAttestationOnceThenExcludesTheProfile() {
        assertEquals(SabrRecoveryAction.REFRESH_ATTESTATION, sabrRecoveryAction(SabrFailureKind.ATTESTATION_REQUIRED, false))
        assertEquals(SabrRecoveryAction.EXCLUDE_PROFILE, sabrRecoveryAction(SabrFailureKind.ATTESTATION_REQUIRED, true))
        assertEquals(SabrRecoveryAction.EXCLUDE_PROFILE, sabrRecoveryAction(SabrFailureKind.PROTOCOL, false))
    }

    @Test
    fun findsProtocolFailureThroughCausesIncludingCycles() {
        val failure = SabrProtocolException("wire error")
        val outer = IllegalStateException("wrapper", failure)
        assertSame(failure, outer.findSabrProtocolException())
        assertTrue(IllegalStateException("unrelated").findSabrProtocolException() == null)

        val cyclic = IllegalStateException("cycle")
        cyclic.initCause(outer)
        failure.initCause(cyclic)
        assertSame(failure, cyclic.findSabrProtocolException())
    }

    private fun bootstrap(poToken: ByteArray = byteArrayOf(3, 4)) =
        SabrBootstrap(
            videoId = "abcdefghijk",
            serverAbrStreamingUrl = "https://www.youtube.com/videoplayback?ump=1",
            videoPlaybackUstreamerConfig = byteArrayOf(1, 2),
            clientName = 1,
            clientVersion = "1.0",
            audioFormat = SabrFormatId(140),
            discardVideoFormat = SabrFormatId(0),
            discardVideoHeight = 0,
            durationMs = 1_000,
            contentLengthBytes = null,
            mimeType = "audio/mp4",
            poToken = poToken,
        )
}
