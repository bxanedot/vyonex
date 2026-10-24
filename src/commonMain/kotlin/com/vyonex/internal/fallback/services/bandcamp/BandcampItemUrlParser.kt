// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object BandcampItemUrlParser {
    fun parse(source: String): String? {
        val response = try {
            Json.parseToJsonElement(source) as? JsonObject ?: throw BandcampItemUrlParseException()
        } catch (error: SerializationException) {
            throw BandcampItemUrlParseException(error)
        }
        return (response["bandcamp_url"] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
    }
}

internal class BandcampItemUrlParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid Bandcamp item URL response", cause)
