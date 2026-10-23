// bxane
package com.vyonex.internal.primary

import com.vyonex.internal.primary.cipher.getTextWithoutRedirects
import com.vyonex.internal.primary.models.YouTubeClient
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.JsonPrimitive

public data class TranscriptCue(
    val text: String,
    val startMs: Long,
    val durationMs: Long,
) {
    override fun toString(): String =
        "TranscriptCue(text=${if (text.isBlank()) "missing" else "present"}, startMs=$startMs, durationMs=$durationMs)"
}

/** Decodes a bounded JSON object without relying on an unbounded content-negotiation read. */
public suspend fun HttpResponse.bodyAsJsonObjectLimited(maxBytes: Int): JsonObject =
    RESPONSE_JSON.parseToJsonElement(bodyAsTextLimited(maxBytes)).jsonObject

/** Requests and parses transcript cues with bounded response, cue-count, and cue-text allocations. */
public suspend fun InnerTube.transcriptCues(
    client: YouTubeClient,
    videoId: String,
): List<TranscriptCue> {
    require(SAFE_VIDEO_ID.matches(videoId)) { "Invalid video ID" }
    val response = getTranscript(client, videoId)
    return parseTranscriptCues(response.bodyAsTextLimited(MAX_TRANSCRIPT_BYTES))
}

internal fun parseTranscriptCues(response: String): List<TranscriptCue> {
    val root = RESPONSE_JSON.parseToJsonElement(response).jsonObject
    val groups =
        (root["actions"] as? JsonArray)
            .orEmpty()
            .asSequence()
            .mapNotNull { action ->
                action.objectAt(
                    "updateEngagementPanelAction",
                    "content",
                    "transcriptRenderer",
                    "body",
                    "transcriptBodyRenderer",
                )?.get("cueGroups") as? JsonArray
            }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
    require(groups.size <= MAX_TRANSCRIPT_CUES) { "Transcript cue count exceeded the supported limit" }
    return groups.mapNotNull { group ->
        val cue =
            (group.objectAt("transcriptCueGroupRenderer")?.get("cues") as? JsonArray)
                ?.firstOrNull()
                ?.objectAt("transcriptCueRenderer") ?: return@mapNotNull null
        val text =
            (cue.objectAt("cue")?.get("simpleText") as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.contentOrNull
                ?.trim('\u266a')
                ?.trim()
                ?.takeIf { it.isNotBlank() && it.length <= MAX_TRANSCRIPT_CUE_TEXT_LENGTH }
                ?: return@mapNotNull null
        TranscriptCue(
            text = text,
            startMs = (cue["startOffsetMs"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null,
            durationMs = (cue["durationMs"] as? JsonPrimitive)?.longOrNull ?: 0L,
        )
    }
}

private fun JsonElement.objectAt(vararg fields: String): JsonObject? =
    fields.fold(this as? JsonObject) { current, field -> current?.get(field) as? JsonObject }

/** Fetches a public YouTube caption track without redirects, cookies, or account headers. */
public suspend fun InnerTube.fetchCaptionText(url: String): String {
    val captionUrl = validatedCaptionUrl(url)
    val response = httpClient.getTextWithoutRedirects(captionUrl, MAX_CAPTION_BYTES)
    if (!response.status.isSuccess()) {
        throw InnerTubeHttpException("fetchCaptionText", response.status)
    }
    return checkNotNull(response.body)
}

private fun validatedCaptionUrl(value: String): Url {
    val url = Url(value)
    val validEndpoint =
        when (url.host) {
            "youtube.com", "www.youtube.com", "music.youtube.com", "m.youtube.com" -> {
                url.encodedPath == "/api/timedtext"
            }

            "video.google.com" -> {
                url.encodedPath == "/timedtext"
            }

            else -> {
                false
            }
        }
    require(url.protocol.name == "https" && url.port == 443 && validEndpoint && url.user == null && url.password == null) {
        "Caption URL must use an approved YouTube HTTPS endpoint"
    }
    return url
}

private val RESPONSE_JSON = Json { ignoreUnknownKeys = true }
private val SAFE_VIDEO_ID = Regex("[A-Za-z0-9_-]{1,64}")
private const val MAX_TRANSCRIPT_BYTES = 4 * 1024 * 1024
private const val MAX_TRANSCRIPT_CUES = 20_000
private const val MAX_TRANSCRIPT_CUE_TEXT_LENGTH = 16 * 1024
private const val MAX_CAPTION_BYTES = 4 * 1024 * 1024
