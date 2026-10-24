// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal data class SoundcloudChannelPayload(
    val profile: SoundcloudProfileInfoItem,
    val visualUrls: List<String>,
)

internal object SoundcloudChannelPayloadParser {
    fun parse(source: String): SoundcloudChannelPayload {
        val user = parseObject(source)
        val visuals = SoundcloudInfoItemPayloadParser.run {
            user.objectValue("visuals").array("visuals").mapNotNull { value ->
                objectValue(value)?.let { it.string("visual_url") }
            }
        }
        return SoundcloudChannelPayload(SoundcloudInfoItemPayloadParser.profile(user), visuals)
    }

    private fun parseObject(source: String): JsonObject = try {
        Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("SoundCloud user response must be a JSON object")
    } catch (error: SerializationException) {
        throw IllegalArgumentException("Invalid SoundCloud user response", error)
    }
}
