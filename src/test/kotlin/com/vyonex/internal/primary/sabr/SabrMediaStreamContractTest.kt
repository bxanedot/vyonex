// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.sabr

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSabrApi::class)
class SabrMediaStreamContractTest {
    @Test
    fun emitsInitializationAndMediaChunksFromAnOfflineUmpResponse() = runBlocking {
        val requests = mutableListOf<String>()
        val requestHeaders = mutableListOf<List<String?>>()
        val client =
            HttpClient(
                MockEngine { request ->
                    requests += request.url.toString()
                    requestHeaders += listOf(
                        request.body.contentType?.toString(),
                        request.headers[HttpHeaders.Accept],
                        request.headers[HttpHeaders.AcceptEncoding],
                        request.headers[HttpHeaders.UserAgent],
                        request.headers[HttpHeaders.Origin],
                        request.headers[HttpHeaders.Referrer],
                    )
                    respond(
                        ByteReadChannel(completeResponse()),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, UMP_CONTENT_TYPE),
                    )
                },
            )
        try {
            val chunks = SabrAudioStream(client, bootstrap()).chunks().toList()

            assertEquals(2, chunks.size)
            assertTrue(chunks[0].isInitialization)
            assertArrayEquals(INIT_BYTES, chunks[0].data)
            assertEquals(null, chunks[0].sequenceNumber)
            assertEquals(null, chunks[0].startMs)
            assertEquals(null, chunks[0].durationMs)
            assertEquals(false, chunks[1].isInitialization)
            assertArrayEquals(MEDIA_BYTES, chunks[1].data)
            assertEquals(1, chunks[1].sequenceNumber)
            assertEquals(0, chunks[1].startMs)
            assertEquals(1_000, chunks[1].durationMs)
            assertEquals(listOf("$ENDPOINT&rn=0"), requests)
            assertEquals(
                listOf("application/x-protobuf", UMP_CONTENT_TYPE, "identity", "VyonEx test agent", "https://www.youtube.com", "https://www.youtube.com/"),
                requestHeaders.single(),
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun buffersOutOfOrderSegmentsAndEmitsThemBySequence() = runBlocking {
        val client =
            HttpClient(
                MockEngine {
                    val shuffled =
                        completeResponse(
                            segments =
                                listOf(
                                    MediaFixture(3, 2, 1_000, 1_000, byteArrayOf(3)),
                                    MediaFixture(2, 1, 0, 1_000, byteArrayOf(1)),
                                ),
                            durationMs = 2_000,
                        )
                    respond(ByteReadChannel(shuffled), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, UMP_CONTENT_TYPE))
                },
            )
        try {
            val chunks = SabrAudioStream(client, bootstrap(durationMs = 2_000)).chunks().toList()

            assertEquals(listOf(null, 1, 2), chunks.map(SabrChunk::sequenceNumber))
            assertArrayEquals(INIT_BYTES, chunks[0].data)
            assertArrayEquals(byteArrayOf(1), chunks[1].data)
            assertArrayEquals(byteArrayOf(3), chunks[2].data)
        } finally {
            client.close()
        }
    }

    @Test
    fun videoTransportUsesTheSelectedVideoFormat() = runBlocking {
        val selectedVideo = SabrFormatId(137, 456, "itag=137")
        val client =
            HttpClient(
                MockEngine {
                    respond(
                        ByteReadChannel(completeResponse(format = selectedVideo)),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, UMP_CONTENT_TYPE),
                    )
                },
            )
        try {
            val videoBootstrap =
                bootstrap().copy(
                    selectedVideoFormat = selectedVideo,
                    selectedVideoHeight = 720,
                    selectedVideoMimeType = "video/mp4",
                )
            val chunks = SabrVideoStream(client, videoBootstrap).chunks().toList()

            assertEquals(listOf(null, 1), chunks.map(SabrChunk::sequenceNumber))
            assertArrayEquals(INIT_BYTES, chunks[0].data)
            assertArrayEquals(MEDIA_BYTES, chunks[1].data)
        } finally {
            client.close()
        }
    }

    @Test
    fun refusesAnUntrustedSabrEndpointBeforeMakingARequest() {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request -> requests += request.url.toString(); error("Unexpected request") })
        try {
            assertThrows(SabrProtocolException::class.java) {
                runBlocking { SabrAudioStream(client, bootstrap(endpoint = "https://attacker.test/videoplayback")).chunks().toList() }
            }
            assertTrue(requests.isEmpty())
        } finally {
            client.close()
        }
    }

    @Test
    fun rejectsRedirectsWithoutRequestingTheRedirectTarget() {
        val requests = mutableListOf<String>()
        val client =
            HttpClient(
                MockEngine { request ->
                    requests += request.url.toString()
                    respond(
                        ByteReadChannel(""),
                        HttpStatusCode.Found,
                        headersOf(HttpHeaders.Location, "https://attacker.test/videoplayback"),
                    )
                },
            )
        try {
            assertThrows(SabrProtocolException::class.java) {
                runBlocking { SabrAudioStream(client, bootstrap()).chunks().toList() }
            }
            assertEquals(listOf("$ENDPOINT&rn=0"), requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun retriesTransientHttpFailureWithBoundedServerDelay() = runBlocking {
        val requests = mutableListOf<String>()
        val delays = mutableListOf<Long>()
        val client =
            HttpClient(
                MockEngine { request ->
                    requests += request.url.toString()
                    if (requests.size == 1) {
                        respond(ByteReadChannel(""), HttpStatusCode.ServiceUnavailable, headersOf(HttpHeaders.RetryAfter, "2"))
                    } else {
                        respond(ByteReadChannel(completeResponse()), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, UMP_CONTENT_TYPE))
                    }
                },
            )
        try {
            val chunks = SabrAudioStream(client, bootstrap(), requestRetryDelay = { delays += it }).chunks().toList()

            assertEquals(2, chunks.size)
            assertEquals(listOf(2_000L), delays)
            assertEquals("0", io.ktor.http.Url(requests[0]).parameters["rn"])
            assertEquals("1", io.ktor.http.Url(requests[1]).parameters["rn"])
        } finally {
            client.close()
        }
    }

    @Test
    fun reportsAttestationRequirementFromServerProtectionStatus() {
        val diagnostics = mutableListOf<SabrResponseDiagnostics>()
        val body = umpPart(UmpPartType.STREAM_PROTECTION_STATUS, ProtoWriter().apply { int32(1, 3) }.toByteArray())
        val client =
            HttpClient(
                MockEngine {
                    respond(ByteReadChannel(body), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, UMP_CONTENT_TYPE))
                },
            )
        try {
            val error =
                assertThrows(SabrProtocolException::class.java) {
                    runBlocking {
                        SabrAudioStream(client, bootstrap(), onResponse = diagnostics::add).chunks().toList()
                    }
                }

            assertEquals(SabrFailureKind.ATTESTATION_REQUIRED, error.kind)
            assertFalse(diagnostics.isEmpty())
            assertEquals(SabrFailureKind.ATTESTATION_REQUIRED.name, diagnostics.last().failureCategory)
        } finally {
            client.close()
        }
    }

    private fun bootstrap(endpoint: String = ENDPOINT, durationMs: Long = 1_000) =
        SabrBootstrap(
            videoId = VIDEO_ID,
            serverAbrStreamingUrl = endpoint,
            videoPlaybackUstreamerConfig = byteArrayOf(1, 2),
            clientName = 1,
            clientVersion = "1.0",
            audioFormat = AUDIO_FORMAT,
            discardVideoFormat = SabrFormatId(18, 55),
            discardVideoHeight = 144,
            durationMs = durationMs,
            contentLengthBytes = null,
            mimeType = "audio/mp4",
            requestUserAgent = "VyonEx test agent",
            requestOrigin = "https://www.youtube.com",
        )

    private fun completeResponse(
        segments: List<MediaFixture> = listOf(MediaFixture(2, 1, 0, 1_000, MEDIA_BYTES)),
        durationMs: Long = 1_000,
        format: SabrFormatId = AUDIO_FORMAT,
    ): ByteArray {
        val initialization =
            ProtoWriter().apply {
                message(2) {
                    int32(1, format.itag)
                    int64(2, format.lastModified)
                    format.xtags?.let { string(3, it) }
                }
                int64(3, durationMs)
                int32(4, segments.maxOf(MediaFixture::sequence))
                int64(9, durationMs)
                int32(10, 1_000)
            }.toByteArray()
        val parts = buildList {
            add(umpPart(UmpPartType.FORMAT_INITIALIZATION_METADATA, initialization))
            add(umpPart(UmpPartType.MEDIA_HEADER, mediaHeader(1, true, 0, 0, 0, INIT_BYTES, format)))
            add(umpPart(UmpPartType.MEDIA, byteArrayOf(1) + INIT_BYTES))
            add(umpPart(UmpPartType.MEDIA_END, byteArrayOf(1)))
            segments.forEach { segment ->
                add(
                    umpPart(
                        UmpPartType.MEDIA_HEADER,
                        mediaHeader(segment.id, false, segment.sequence, segment.startMs, segment.durationMs, segment.data, format),
                    ),
                )
                add(umpPart(UmpPartType.MEDIA, byteArrayOf(segment.id.toByte()) + segment.data))
                add(umpPart(UmpPartType.MEDIA_END, byteArrayOf(segment.id.toByte())))
            }
            add(umpPart(UmpPartType.END_OF_TRACK, byteArrayOf()))
        }
        return parts.fold(ByteArray(0)) { output, part -> output + part }
    }

    private fun mediaHeader(
        id: Int,
        isInitialization: Boolean,
        sequence: Int,
        startMs: Long,
        durationMs: Long,
        data: ByteArray,
        format: SabrFormatId = AUDIO_FORMAT,
    ): ByteArray =
        ProtoWriter().apply {
            int32(1, id)
            string(2, VIDEO_ID)
            bool(8, isInitialization)
            int32(9, sequence)
            int64(11, startMs)
            int64(12, durationMs)
            int64(14, data.size.toLong())
            message(13) {
                int32(1, format.itag)
                int64(2, format.lastModified)
                format.xtags?.let { string(3, it) }
            }
        }.toByteArray()

    private fun umpPart(type: Int, payload: ByteArray): ByteArray {
        require(type < 128 && payload.size < 128)
        return byteArrayOf(type.toByte(), payload.size.toByte()) + payload
    }

    private data class MediaFixture(
        val id: Int,
        val sequence: Int,
        val startMs: Long,
        val durationMs: Long,
        val data: ByteArray,
    )

    private companion object {
        const val VIDEO_ID = "abcdefghijk"
        const val ENDPOINT = "https://r.googlevideo.com/videoplayback?itag=140"
        const val UMP_CONTENT_TYPE = "application/vnd.yt-ump"
        val AUDIO_FORMAT = SabrFormatId(140, 123)
        val INIT_BYTES = byteArrayOf(10, 11)
        val MEDIA_BYTES = byteArrayOf(20, 21, 22)
    }
}
