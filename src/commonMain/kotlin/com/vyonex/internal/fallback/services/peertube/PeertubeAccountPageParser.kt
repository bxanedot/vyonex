// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal data class PeertubeAccountPagePayload(
    val id: String,
    val displayName: String?,
    val description: String?,
    val followerCount: Long,
    val error: String?,
    val avatars: List<PeertubeImageReference>,
    val banners: List<PeertubeImageReference>,
)

internal object PeertubeAccountPageParser {
    fun parse(source: String): PeertubeAccountPagePayload {
        val response = try {
            Json.parseToJsonElement(source) as? JsonObject ?: throw PeertubeAccountPageParseException()
        } catch (error: SerializationException) {
            throw PeertubeAccountPageParseException(error)
        }
        return PeertubeAccountPagePayload(
            id = response["id"]?.let { (it as? JsonPrimitive)?.content ?: it.toString() } ?: "null",
            displayName = response.string("displayName"),
            description = response.string("description"),
            followerCount = response.long("followersCount", 0),
            error = response.string("error")?.takeIf(String::isNotBlank),
            avatars = PeertubeImageParser.profileImages(response, "avatars", "avatar"),
            banners = PeertubeImageParser.profileImages(response, "banners", "banner"),
        )
    }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.long(name: String, fallback: Long): Long =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: fallback
}

internal class PeertubeAccountPageParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid PeerTube account data", cause)
