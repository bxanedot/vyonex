// bxane
package com.vyonex.internal.primary

import com.vyonex.internal.primary.models.YouTubeClient
import com.vyonex.internal.primary.models.YouTubeLocale
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        val requestBodies = mutableListOf<String>()
        val client =
            HttpClient(
                MockEngine { request ->
                    requests += request.url.toString()
                    requestBodies += (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
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
        innerTube.locale = YouTubeLocale(gl = "CH", hl = "fr")
        try {
            val cues = innerTube.transcriptCues(YouTubeClient.WEB, VIDEO_ID)

            assertEquals(listOf(TranscriptCue("Hello", 120L, 400L)), cues)
            assertEquals(1, requests.size)
            assertTrue(requests.single().startsWith("https://music.youtube.com/youtubei/v1/get_transcript"))
            val body = Json.parseToJsonElement(requestBodies.single()).jsonObject
            val context = body.getValue("context").jsonObject
            val contextClient = context.getValue("client").jsonObject
            assertEquals("WEB", contextClient.getValue("clientName").jsonPrimitive.content)
            assertEquals("CH", contextClient.getValue("gl").jsonPrimitive.content)
            assertEquals("fr", contextClient.getValue("hl").jsonPrimitive.content)
            assertEquals(
                Json.parseToJsonElement("""{"internalExperimentFlags":[],"useSsl":true}"""),
                context.getValue("request"),
            )
            assertEquals(
                Json.parseToJsonElement("""{"lockedSafetyMode":false}"""),
                context.getValue("user"),
            )
            assertEquals(
                "\n\u000B$VIDEO_ID",
                java.util.Base64.getDecoder().decode(body.getValue("params").jsonPrimitive.content).decodeToString(),
            )
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
