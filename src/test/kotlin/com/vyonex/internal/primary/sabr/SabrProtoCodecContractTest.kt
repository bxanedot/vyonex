// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SabrProtoCodecContractTest {
    @Test
    fun decodesMediaAndInitializationMetadata() {
        val encodedFormat = ProtoWriter().apply { int32(1, 137); int64(2, 1_234); string(3, "itag=137") }.toByteArray()
        val header =
            ProtoWriter().apply {
                int32(1, 4)
                string(2, "abcdefghijk")
                int64(6, 99)
                int32(7, 0)
                bool(8, true)
                int32(9, 3)
                bytes(13, encodedFormat)
                int64(14, 42)
                message(15) {
                    int64(1, 1_500)
                    int64(2, 2_500)
                    int32(3, 1_000)
                }
            }.toByteArray()

        assertEquals(
            SabrMediaHeader(
                headerId = 4,
                videoId = "abcdefghijk",
                formatId = SabrFormatId(137, 1_234, "itag=137"),
                startRange = 99,
                isInitSegment = true,
                sequenceNumber = 3,
                startMs = 1_500,
                durationMs = 2_500,
                contentLength = 42,
            ),
            SabrProtoCodec.decodeMediaHeader(header),
        )
        val initialization =
            ProtoWriter().apply {
                bytes(2, encodedFormat)
                int64(3, 8_000)
                int32(4, 6)
                int64(9, 3_000)
                int32(10, 1_000)
            }.toByteArray()
        assertEquals(
            SabrFormatInitialization(SabrFormatId(137, 1_234, "itag=137"), 8_000, 6, 3_000),
            SabrProtoCodec.decodeFormatInitialization(initialization),
        )
        assertNull(SabrProtoCodec.decodeFormatInitialization(byteArrayOf()))
    }

    @Test
    fun decodesPolicyAndControlEvents() {
        val policy =
            ProtoWriter().apply {
                int64(1, 5_000)
                int64(3, 10_000)
                int64(4, 500)
                int64(5, 2_000)
                bytes(7, byteArrayOf(7, 8))
            }.toByteArray()
        val next = SabrProtoCodec.decodeNextRequestPolicy(policy)
        assertEquals(5_000, next.targetAudioReadaheadMs)
        assertEquals(10_000, next.maxTimeSinceLastRequestMs)
        assertEquals(500, next.backoffTimeMs)
        assertEquals(2_000, next.minAudioReadaheadMs)
        assertArrayEquals(byteArrayOf(7, 8), next.playbackCookie)
        assertEquals(
            "https://example.test/next",
            SabrProtoCodec.decodeRedirect(ProtoWriter().apply { string(1, "https://example.test/next") }.toByteArray()),
        )
        assertNull(SabrProtoCodec.decodeRedirect(byteArrayOf()))
        assertEquals(SabrEvent.Error("UNAVAILABLE", 503), SabrProtoCodec.decodeError(ProtoWriter().apply { string(1, "UNAVAILABLE"); int32(2, 503) }.toByteArray()))
        assertEquals(
            SabrContext(9, byteArrayOf(3, 4), true, 2),
            SabrProtoCodec.decodeContextUpdate(
                ProtoWriter().apply { int32(1, 9); bytes(3, byteArrayOf(3, 4)); bool(4, true); int32(5, 2) }.toByteArray(),
            ),
        )
        assertNull(SabrProtoCodec.decodeContextUpdate(byteArrayOf()))
        assertEquals(
            SabrEvent.StreamProtectionStatus(2, 4),
            SabrProtoCodec.decodeStreamProtectionStatus(ProtoWriter().apply { int32(1, 2); int32(2, 4) }.toByteArray()),
        )
    }

    @Test
    fun decodesPackedAndUnpackedContextPoliciesWithAnEntryLimit() {
        val policy =
            ProtoWriter().apply {
                int32(1, 1)
                packedInt32(1, listOf(2, 3))
                packedInt32(2, listOf(4))
                int32(3, 5)
            }.toByteArray()

        assertEquals(SabrEvent.ContextSendingPolicy(setOf(1, 2, 3), setOf(4), setOf(5)), SabrProtoCodec.decodeContextSendingPolicy(policy))
        val tooMany = ProtoWriter().apply { packedInt32(1, (1..257).toList()) }.toByteArray()
        assertThrows(SabrProtocolException::class.java) { SabrProtoCodec.decodeContextSendingPolicy(tooMany) }
    }

    @Test
    fun encodesRequestFormatsContextsAndPlaybackCookie() {
        val audio = SabrFormatId(140, 11, "audio")
        val video = SabrFormatId(248, 22, "video")
        val bootstrap =
            SabrBootstrap(
                videoId = "abcdefghijk",
                serverAbrStreamingUrl = "https://r1.googlevideo.com/videoplayback",
                videoPlaybackUstreamerConfig = byteArrayOf(1, 2),
                clientName = 67,
                clientVersion = "1.0",
                audioFormat = audio,
                discardVideoFormat = SabrFormatId(137, 33),
                discardVideoHeight = 360,
                selectedVideoFormat = video,
                selectedVideoHeight = 720,
                durationMs = 10_000,
                contentLengthBytes = 100,
                mimeType = "audio/webm",
                poToken = byteArrayOf(9),
            )
        val context = SabrContext(5, byteArrayOf(6), true, 0)
        val encoded =
            SabrProtoCodec.encodeRequest(
                bootstrap = bootstrap,
                playerTimeMs = 2_500,
                initialized = true,
                bufferedRanges = listOf(SabrBufferedRange(audio, 0, 1_000, 0, 1)),
                playbackCookie = byteArrayOf(8),
                contexts = listOf(context, SabrContext(7, byteArrayOf(7), false, 0)),
                activeContextTypes = setOf(5),
            )
        val top = lengthDelimitedFields(encoded)
        val session = lengthDelimitedFields(top.getValue(19).single())
        val activeContext = session.getValue(5).single()
        val contextReader = ProtoReader(activeContext)
        var contextType = 0
        var contextValue = byteArrayOf()
        while (contextReader.hasRemaining) {
            val tag = contextReader.tag()
            when (tag.field) {
                1 -> contextType = contextReader.varint().toInt()
                2 -> contextValue = contextReader.bytes()
                else -> contextReader.skip(tag)
            }
        }

        assertArrayEquals(byteArrayOf(1, 2), top.getValue(5).single())
        assertEquals(2, top.getValue(2).size)
        assertEquals(audio, decodeFormatId(top.getValue(16).single()))
        assertEquals(bootstrap.discardVideoFormat, decodeFormatId(top.getValue(17).single()))
        assertArrayEquals(byteArrayOf(9), session.getValue(2).single())
        assertArrayEquals(byteArrayOf(8), session.getValue(3).single())
        assertEquals(5, contextType)
        assertArrayEquals(byteArrayOf(6), contextValue)

        val videoTop =
            lengthDelimitedFields(
                SabrProtoCodec.encodeRequest(
                    bootstrap = bootstrap,
                    playerTimeMs = 0,
                    initialized = false,
                    bufferedRanges = emptyList(),
                    playbackCookie = null,
                    contexts = emptyList(),
                    mediaType = SabrMediaType.VIDEO,
                ),
            )
        assertEquals(audio, decodeFormatId(videoTop.getValue(16).single()))
        assertEquals(video, decodeFormatId(videoTop.getValue(17).single()))
    }

    @Test
    fun videoRequestsRequireTheSelectedTrack() {
        val bootstrap =
            SabrBootstrap(
                videoId = "abcdefghijk",
                serverAbrStreamingUrl = "https://r1.googlevideo.com/videoplayback",
                videoPlaybackUstreamerConfig = byteArrayOf(),
                clientName = 67,
                clientVersion = "1.0",
                audioFormat = SabrFormatId(140, 11),
                discardVideoFormat = SabrFormatId(137, 33),
                discardVideoHeight = 360,
                durationMs = 10_000,
                contentLengthBytes = null,
                mimeType = "audio/webm",
            )
        assertThrows(IllegalArgumentException::class.java) {
            SabrProtoCodec.encodeRequest(bootstrap, 0, false, emptyList(), null, emptyList(), mediaType = SabrMediaType.VIDEO)
        }
    }

    private fun lengthDelimitedFields(data: ByteArray): Map<Int, List<ByteArray>> {
        val fields = mutableMapOf<Int, MutableList<ByteArray>>()
        val reader = ProtoReader(data)
        while (reader.hasRemaining) {
            val tag = reader.tag()
            if (tag.wireType == 2) fields.getOrPut(tag.field, ::mutableListOf) += reader.bytes() else reader.skip(tag)
        }
        return fields
    }

    private fun decodeFormatId(data: ByteArray): SabrFormatId {
        val reader = ProtoReader(data)
        var itag = 0
        var lastModified = 0L
        var xtags: String? = null
        while (reader.hasRemaining) {
            val tag = reader.tag()
            when (tag.field) {
                1 -> itag = reader.varint().toInt()
                2 -> lastModified = reader.varint()
                3 -> xtags = reader.string()
                else -> reader.skip(tag)
            }
        }
        return SabrFormatId(itag, lastModified, xtags)
    }
}
