// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal data class SoundcloudUsersPagePayload(
    val users: List<SoundcloudProfileInfoItem>,
    val nextPageUrl: String?,
)

internal object SoundcloudUsersPageParser {
    fun parse(source: String): SoundcloudUsersPagePayload {
        val response = parseObject(source)
        return SoundcloudUsersPagePayload(
            users = SoundcloudInfoItemPayloadParser.run {
                response.array("collection").mapNotNull { objectValue(it)?.let(::profile) }
            },
            nextPageUrl = SoundcloudInfoItemPayloadParser.run { response.string("next_href") },
        )
    }

    private fun parseObject(source: String): JsonObject = try {
        Json.parseToJsonElement(source) as? JsonObject
            ?: throw IllegalArgumentException("SoundCloud users response must be a JSON object")
    } catch (error: SerializationException) {
        throw IllegalArgumentException("Invalid SoundCloud users response", error)
    }
}
