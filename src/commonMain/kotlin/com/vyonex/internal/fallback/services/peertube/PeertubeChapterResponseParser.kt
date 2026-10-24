// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal data class PeertubeChapter(val title: String, val timecodeSeconds: Int)

internal object PeertubeChapterResponseParser {
    fun parse(source: String): List<PeertubeChapter> {
        val response = Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("Chapter response must be a JSON object")
        return response.array("chapters").mapNotNull(::chapter)
    }

    private fun chapter(value: JsonElement): PeertubeChapter? {
        val chapter = value as? JsonObject ?: return null
        val timecode = chapter.long("timecode", -1)
        if (timecode !in 0..Int.MAX_VALUE.toLong()) return null
        return PeertubeChapter(chapter.string("title") ?: "", timecode.toInt())
    }

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.long(name: String, fallback: Long): Long =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: fallback
}
