// bxane
package com.vyonex.internal.primary

import com.vyonex.internal.primary.models.YouTubeClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CaptionsContractTest {
    @Test
    fun transcriptReaderSkipsMalformedCueGroupsAndDefaultsMissingDuration() {
        val response =
            """{"actions":[{"updateEngagementPanelAction":{"content":{"transcriptRenderer":{"body":{"transcriptBodyRenderer":{"cueGroups":[]}}}}}},{"updateEngagementPanelAction":{"content":{"transcriptRenderer":{"body":{"transcriptBodyRenderer":{"cueGroups":[{"transcriptCueGroupRenderer":{"cues":[{"transcriptCueRenderer":{"cue":{"simpleText":"♪ First ♪"}}}]}},{"transcriptCueGroupRenderer":{"cues":[{"transcriptCueRenderer":{"cue":{"simpleText":" Second "},"startOffsetMs":80}}]}}]}}}}}}]}"""

        assertEquals(listOf(TranscriptCue("Second", 80L, 0L)), parseTranscriptCues(response))
    }

    @Test
    fun transcriptReaderRejectsExcessiveCueGroups() {
        val groups = List(20_001) { "{}" }.joinToString(",")
        val response = """{"actions":[{"updateEngagementPanelAction":{"content":{"transcriptRenderer":{"body":{"transcriptBodyRenderer":{"cueGroups":[$groups]}}}}}}]}"""

        assertThrows(IllegalArgumentException::class.java) { parseTranscriptCues(response) }
    }

    @Test
    fun transcriptReaderSkipsActionsWithoutTranscriptPanels() = runBlocking {
        val requests = mutableListOf<String>()
        val client =
            HttpClient(
                MockEngine { request ->
                    requests += request.url.toString()
                    respond(
                        ByteReadChannel(TRANSCRIPT_JSON),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }) }
            }
        val innerTube = InnerTube(client)
        try {
            val cues = innerTube.transcriptCues(YouTubeClient.WEB, VIDEO_ID)

            assertEquals(listOf(TranscriptCue("Hello", 120L, 400L)), cues)
            assertEquals(1, requests.size)
            assertTrue(requests.single().startsWith("https://music.youtube.com/youtubei/v1/get_transcript"))
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun invalidVideoIdIsRejectedBeforeRequest() {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request -> requests += request.url.toString(); error("No request expected") })
        val innerTube = InnerTube(client)
        try {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { innerTube.transcriptCues(YouTubeClient.WEB, "bad/id") }
            }
            assertTrue(requests.isEmpty())
        } finally {
            innerTube.close()
            client.close()
        }
    }

    @Test
    fun publicCaptionFetchIsBoundToApprovedHostAndDoesNotFollowRedirects() = runBlocking {
        val requests = mutableListOf<String>()
        val client =
            HttpClient(
                MockEngine { request ->
                    requests += request.url.toString()
                    respond(
                        ByteReadChannel(""),
                        HttpStatusCode.Found,
                        headersOf(HttpHeaders.Location, "https://attacker.test/caption"),
                    )
                },
            )
        val innerTube = InnerTube(client)
        try {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { innerTube.fetchCaptionText("https://attacker.test/api/timedtext") }
            }
            assertTrue(requests.isEmpty())

            assertThrows(InnerTubeHttpException::class.java) {
                runBlocking { innerTube.fetchCaptionText("https://www.youtube.com/api/timedtext?v=$VIDEO_ID") }
            }
            assertEquals(listOf("https://www.youtube.com/api/timedtext?v=$VIDEO_ID"), requests)
            assertFalse(requests.any { it.contains("attacker.test") })
        } finally {
            innerTube.close()
            client.close()
        }
    }

    private companion object {
        const val VIDEO_ID = "abcdefghijk"
        const val TRANSCRIPT_JSON = """{"actions":[{}, {"updateEngagementPanelAction":{"content":{"transcriptRenderer":{"body":{"transcriptBodyRenderer":{"cueGroups":[{"transcriptCueGroupRenderer":{"cues":[{"transcriptCueRenderer":{"cue":{"simpleText":"♪ Hello ♪"},"startOffsetMs":120,"durationMs":400}}]}}]}}}}}}]}"""
    }
}
