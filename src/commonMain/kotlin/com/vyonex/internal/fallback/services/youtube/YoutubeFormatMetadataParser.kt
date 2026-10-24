// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.youtube

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

internal data class YoutubeFormatMetadata(
    val id: Int,
    val mimeType: String,
    val averageBitrate: Int,
    val resolution: String,
    val fps: Int,
)

internal object YoutubeFormatMetadataParser {
    fun parse(source: String, audio: Boolean): YoutubeFormatMetadata {
        val format = Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("Stream format must be a JSON object")
        val mimeType = format.string("mimeType").substringBefore(';').trim()
        val height = format.number("height", 0)
        val quality = format.string("qualityLabel")
        val resolution = quality.ifBlank {
            height.takeIf { it > 0 }?.let { "${it}p" }.orEmpty()
        }
        return YoutubeFormatMetadata(
            id = format.number("itag", 0),
            mimeType = mimeType,
            averageBitrate = if (audio) format.number("averageBitrate", -1) else -1,
            resolution = if (audio) "" else resolution,
            fps = if (audio) -1 else format.number("fps", -1),
        )
    }

    private fun JsonObject.string(name: String): String =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()

    private fun JsonObject.number(name: String, fallback: Int): Int =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.doubleOrNull?.toInt() ?: fallback
}
