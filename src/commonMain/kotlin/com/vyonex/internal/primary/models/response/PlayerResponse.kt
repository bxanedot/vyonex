// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.models.response

import com.vyonex.internal.primary.models.Thumbnail
import com.vyonex.internal.primary.models.Thumbnails
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

data class PlayerResponse(
    val playabilityStatus: PlayabilityStatus,
    val streamingData: StreamingData? = null,
    val videoDetails: VideoDetails? = null,
    val playbackTracking: PlaybackTracking? = null,
    val playerConfig: PlayerConfig? = null,
) {
    companion object {
        internal fun parse(payload: String): PlayerResponse =
            Json.parseToJsonElement(payload).jsonObject.let { root ->
                PlayerResponse(
                    playabilityStatus = root.requiredObject("playabilityStatus").let { PlayabilityStatus(it.requiredString("status"), it.nullableString("reason")) },
                    streamingData = root.objectValue("streamingData")?.toStreamingData(),
                    videoDetails = root.objectValue("videoDetails")?.toVideoDetails(),
                    playbackTracking = root.objectValue("playbackTracking")?.toPlaybackTracking(),
                    playerConfig = root.objectValue("playerConfig")?.toPlayerConfig(),
                )
            }
    }

    data class PlayabilityStatus(val status: String, val reason: String? = null)

    data class StreamingData(
        val formats: List<Format>? = null,
        val adaptiveFormats: List<Format> = emptyList(),
        val expiresInSeconds: Int? = null,
        val hlsManifestUrl: String? = null,
        val dashManifestUrl: String? = null,
        val serverAbrStreamingUrl: String? = null,
    ) {
        data class Format(
            val itag: Int = -1,
            val url: String? = null,
            val mimeType: String = "",
            val bitrate: Int = 0,
            val averageBitrate: Int? = null,
            val width: Int? = null,
            val height: Int? = null,
            val contentLength: Long? = null,
            val quality: String? = null,
            val audioQuality: String? = null,
            val approxDurationMs: String? = null,
            val audioSampleRate: Int? = null,
            val audioChannels: Int? = null,
            val loudnessDb: Double? = null,
            val lastModified: String? = null,
            val xtags: String? = null,
            val audioTrack: AudioTrack? = null,
            val isDrc: Boolean = false,
            val signatureCipher: String? = null,
            val cipher: String? = null,
        ) {
            val isAudio: Boolean
                get() = width == null

            data class AudioTrack(
                val id: String? = null,
                val displayName: String? = null,
                val audioIsDefault: Boolean? = null,
            )
        }
    }

    data class PlayerConfig(
        val mediaCommonConfig: MediaCommonConfig? = null,
    ) {
        var audioConfig: AudioConfig? = null
            private set

        internal constructor(
            mediaCommonConfig: MediaCommonConfig?,
            audioConfig: AudioConfig?,
        ) : this(mediaCommonConfig) {
            this.audioConfig = audioConfig
        }

        data class AudioConfig(
            val loudnessDb: Double? = null,
            val perceptualLoudnessDb: Double? = null,
        )

        data class MediaCommonConfig(
            val mediaUstreamerRequestConfig: MediaUstreamerRequestConfig? = null,
        ) {
            data class MediaUstreamerRequestConfig(
                val videoPlaybackUstreamerConfig: String? = null,
            )
        }
    }

    data class VideoDetails(
        val videoId: String = "",
        val title: String? = null,
        val author: String? = null,
        val channelId: String? = null,
        val lengthSeconds: String? = null,
        val musicVideoType: String? = null,
        val viewCount: String? = null,
        val thumbnail: Thumbnails? = null,
        val isLiveContent: Boolean? = null,
    )

    data class PlaybackTracking(
        val videostatsPlaybackUrl: VideostatsUrl? = null,
        val videostatsWatchtimeUrl: VideostatsUrl? = null,
        val atrUrl: VideostatsUrl? = null,
        val videostatsScheduledFlushWalltimeSeconds: List<Int>? = null,
        val videostatsDefaultFlushIntervalSeconds: Int? = null,
    ) {
        data class VideostatsUrl(val baseUrl: String)
    }
}

private fun JsonObject.toStreamingData() =
    PlayerResponse.StreamingData(
        formats = objectList("formats")?.map(JsonObject::toFormat),
        adaptiveFormats = objectList("adaptiveFormats", emptyWhenMissing = true).orEmpty().map(JsonObject::toFormat),
        expiresInSeconds = nullableInt("expiresInSeconds"),
        hlsManifestUrl = nullableString("hlsManifestUrl"),
        dashManifestUrl = nullableString("dashManifestUrl"),
        serverAbrStreamingUrl = nullableString("serverAbrStreamingUrl"),
    )

private fun JsonObject.toFormat() =
    PlayerResponse.StreamingData.Format(
        itag = int("itag", -1),
        url = nullableString("url"),
        mimeType = string("mimeType", ""),
        bitrate = int("bitrate", 0),
        averageBitrate = nullableInt("averageBitrate"),
        width = nullableInt("width"),
        height = nullableInt("height"),
        contentLength = nullableLong("contentLength"),
        quality = nullableString("quality"),
        audioQuality = nullableString("audioQuality"),
        approxDurationMs = nullableString("approxDurationMs"),
        audioSampleRate = nullableInt("audioSampleRate"),
        audioChannels = nullableInt("audioChannels"),
        loudnessDb = nullableDouble("loudnessDb"),
        lastModified = nullableString("lastModified"),
        xtags = nullableString("xtags"),
        audioTrack = objectValue("audioTrack")?.let { track ->
            PlayerResponse.StreamingData.Format.AudioTrack(
                id = track.nullableString("id"),
                displayName = track.nullableString("displayName"),
                audioIsDefault = track.nullableBoolean("audioIsDefault"),
            )
        },
        isDrc = boolean("isDrc", false),
        signatureCipher = nullableString("signatureCipher") ?: nullableString("signature_cipher"),
        cipher = nullableString("cipher"),
    )

private fun JsonObject.toPlayerConfig() =
    PlayerResponse.PlayerConfig(
        mediaCommonConfig = objectValue("mediaCommonConfig")?.let { common ->
            PlayerResponse.PlayerConfig.MediaCommonConfig(
                mediaUstreamerRequestConfig = common.objectValue("mediaUstreamerRequestConfig")?.let { request ->
                    PlayerResponse.PlayerConfig.MediaCommonConfig.MediaUstreamerRequestConfig(
                        videoPlaybackUstreamerConfig = request.nullableString("videoPlaybackUstreamerConfig"),
                    )
                },
            )
        },
        audioConfig = objectValue("audioConfig")?.let { audio ->
            PlayerResponse.PlayerConfig.AudioConfig(
                loudnessDb = audio.nullableDouble("loudnessDb"),
                perceptualLoudnessDb = audio.nullableDouble("perceptualLoudnessDb"),
            )
        },
    )

private fun JsonObject.toVideoDetails() =
    PlayerResponse.VideoDetails(
        videoId = string("videoId", ""),
        title = nullableString("title"),
        author = nullableString("author"),
        channelId = nullableString("channelId"),
        lengthSeconds = nullableString("lengthSeconds"),
        musicVideoType = nullableString("musicVideoType"),
        viewCount = nullableString("viewCount"),
        thumbnail = objectValue("thumbnail")?.let { thumbs ->
            Thumbnails(
                thumbs.objectList("thumbnails")?.map { item ->
                    Thumbnail(item.requiredString("url"), item.nullableInt("width"), item.nullableInt("height"))
                } ?: error("Missing thumbnail list"),
            )
        },
        isLiveContent = nullableBoolean("isLiveContent"),
    )

private fun JsonObject.toPlaybackTracking() =
    PlayerResponse.PlaybackTracking(
        videostatsPlaybackUrl = objectValue("videostatsPlaybackUrl")?.toVideostatsUrl(),
        videostatsWatchtimeUrl = objectValue("videostatsWatchtimeUrl")?.toVideostatsUrl(),
        atrUrl = objectValue("atrUrl")?.toVideostatsUrl(),
        videostatsScheduledFlushWalltimeSeconds = intList("videostatsScheduledFlushWalltimeSeconds"),
        videostatsDefaultFlushIntervalSeconds = nullableInt("videostatsDefaultFlushIntervalSeconds"),
    )

private fun JsonObject.toVideostatsUrl() = PlayerResponse.PlaybackTracking.VideostatsUrl(requiredString("baseUrl"))

private fun JsonObject.requiredObject(name: String): JsonObject =
    objectValue(name) ?: error("Missing object: $name")

private fun JsonObject.objectValue(name: String): JsonObject? =
    when (val value = this[name]) {
        null, JsonNull -> null
        is JsonObject -> value
        else -> error("Expected object: $name")
    }

private fun JsonObject.objectList(
    name: String,
    emptyWhenMissing: Boolean = false,
): List<JsonObject>? =
    when (val value = this[name]) {
        null -> if (emptyWhenMissing) emptyList() else null
        JsonNull -> if (emptyWhenMissing) error("Expected array: $name") else null
        is JsonArray -> value.map(JsonElement::objectValue)
        else -> error("Expected array: $name")
    }

private fun JsonObject.intList(name: String): List<Int>? =
    when (val value = this[name]) {
        null, JsonNull -> null
        is JsonArray -> value.map(JsonElement::intValue)
        else -> error("Expected array: $name")
    }

private fun JsonObject.requiredString(name: String): String = this[name].stringValue()

private fun JsonObject.string(
    name: String,
    default: String,
): String = if (name in this) requiredString(name) else default

private fun JsonObject.nullableString(name: String): String? =
    when (val value = this[name]) {
        null, JsonNull -> null
        else -> value.stringValue()
    }

private fun JsonElement?.stringValue(): String =
    (this as? JsonPrimitive)?.takeIf { it.isString }?.content ?: error("Expected string")

private fun JsonObject.int(
    name: String,
    default: Int,
): Int = if (name in this) getValue(name).intValue() else default

private fun JsonObject.nullableInt(name: String): Int? = nullable(name, JsonElement::intValue)

private fun JsonObject.nullableLong(name: String): Long? = nullable(name, JsonElement::longValue)

private fun JsonObject.nullableDouble(name: String): Double? = nullable(name, JsonElement::doubleValue)

private fun JsonObject.nullableBoolean(name: String): Boolean? = nullable(name, JsonElement::booleanValue)

private fun JsonObject.boolean(
    name: String,
    default: Boolean,
): Boolean = if (name in this) getValue(name).booleanValue() else default

private inline fun <T> JsonObject.nullable(
    name: String,
    decode: (JsonElement) -> T,
): T? =
    when (val value = this[name]) {
        null, JsonNull -> null
        else -> decode(value)
    }

private fun JsonElement.intValue(): Int =
    (this as? JsonPrimitive)?.intOrNull ?: error("Expected integer")

private fun JsonElement.longValue(): Long =
    (this as? JsonPrimitive)?.longOrNull ?: error("Expected integer")

private fun JsonElement.doubleValue(): Double =
    (this as? JsonPrimitive)?.doubleOrNull ?: error("Expected number")

private fun JsonElement.booleanValue(): Boolean =
    (this as? JsonPrimitive)?.booleanOrNull ?: error("Expected boolean")

private fun JsonElement.objectValue(): JsonObject = this as? JsonObject ?: error("Expected object")
