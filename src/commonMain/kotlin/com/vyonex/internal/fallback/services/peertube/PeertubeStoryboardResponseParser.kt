// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal data class PeertubeStoryboard(
    val imagePath: String,
    val frameWidth: Int,
    val frameHeight: Int,
    val totalCount: Int,
    val durationPerFrame: Int,
    val framesPerPageX: Int,
    val framesPerPageY: Int,
)

internal object PeertubeStoryboardResponseParser {
    fun parse(source: String): List<PeertubeStoryboard> {
        val response = Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("Storyboard response must be a JSON object")
        return response.array("storyboards").mapNotNull(::storyboard)
    }

    private fun storyboard(value: JsonElement): PeertubeStoryboard? {
        val storyboard = value as? JsonObject ?: return null
        val width = storyboard.number("spriteWidth", 0)
        val height = storyboard.number("spriteHeight", 0)
        val totalWidth = storyboard.number("totalWidth", 0)
        val totalHeight = storyboard.number("totalHeight", 0)
        val duration = storyboard.number("spriteDuration", 0)
        val image = storyboard.string("fileUrl")?.takeIf(String::isNotBlank)
            ?: storyboard.string("storyboardPath")?.takeIf(String::isNotBlank)
        if (image == null || width <= 0 || height <= 0 || duration <= 0
            || totalWidth < width || totalHeight < height
        ) return null

        val columns = totalWidth / width
        val rows = totalHeight / height
        val count = columns.toLong() * rows
        if (count > Int.MAX_VALUE) return null
        return PeertubeStoryboard(image, width, height, count.toInt(), duration, columns, rows)
    }

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(name: String, fallback: Int): Int =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.intOrNull ?: it.longOrNull?.toInt() ?: it.doubleOrNull?.toInt() } ?: fallback
}
