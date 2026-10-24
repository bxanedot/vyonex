// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

internal enum class PeertubeImageSize { LOW, MEDIUM, UNKNOWN }

internal data class PeertubeImageReference(
    val path: String,
    val width: Int = -1,
    val size: PeertubeImageSize = PeertubeImageSize.UNKNOWN,
)

internal object PeertubeImageParser {
    fun thumbnails(thumbnailPath: String?, previewPath: String?): List<PeertubeImageReference> =
        listOfNotNull(
            thumbnailPath?.takeIf(String::isNotBlank)?.let {
                PeertubeImageReference(it, size = PeertubeImageSize.LOW)
            },
            previewPath?.takeIf(String::isNotBlank)?.let {
                PeertubeImageReference(it, size = PeertubeImageSize.MEDIUM)
            },
        )

    fun profileImages(source: String, listKey: String, fallbackKey: String): List<PeertubeImageReference> {
        val item = Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("Image response must be a JSON object")
        val values = item.array(listKey)
        return if (values.isEmpty()) {
            listOfNotNull(item[fallbackKey]?.let(::profileImage))
        } else {
            values.mapNotNull { (it as? JsonObject)?.let(::profileImage) }
        }
    }

    private fun profileImage(value: JsonElement): PeertubeImageReference? {
        val image = value as? JsonObject ?: return null
        val path = image.string("path")?.takeIf(String::isNotBlank) ?: return null
        return PeertubeImageReference(path, image.number("width", -1))
    }

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(name: String, fallback: Int): Int =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.doubleOrNull?.toInt() ?: fallback
}
