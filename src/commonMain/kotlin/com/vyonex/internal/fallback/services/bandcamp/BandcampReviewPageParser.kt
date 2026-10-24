// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal data class BandcampReviewPayload(
    val text: String,
    val uploaderName: String,
    val avatarId: Long,
    val token: String?,
)

internal data class BandcampReviewPagePayload(
    val reviews: List<BandcampReviewPayload>,
    val moreAvailable: Boolean,
    val nextToken: String?,
)

internal object BandcampReviewPageParser {
    fun parseEmbedded(source: String): BandcampReviewPagePayload =
        parse(source, "reviews", "more_reviews_available")

    fun parseResponse(source: String): BandcampReviewPagePayload =
        parse(source, "results", "more_available")

    fun parseTrackId(source: String): Long = parseObject(source).long("item_id", 0)

    private fun parse(source: String, reviewsKey: String, moreKey: String): BandcampReviewPagePayload {
        val root = parseObject(source)
        val reviews = root.array(reviewsKey).mapNotNull(::review)
        return BandcampReviewPagePayload(
            reviews = reviews,
            moreAvailable = root.boolean(moreKey),
            nextToken = reviews.mapNotNull(BandcampReviewPayload::token).lastOrNull(),
        )
    }

    private fun parseObject(source: String): JsonObject = try {
        Json.parseToJsonElement(source) as? JsonObject ?: throw BandcampReviewPageParseException()
    } catch (error: SerializationException) {
        throw BandcampReviewPageParseException(error)
    }

    private fun review(value: JsonElement): BandcampReviewPayload? {
        val review = value as? JsonObject ?: return null
        return BandcampReviewPayload(
            text = review.string("why") ?: "",
            uploaderName = review.string("name") ?: "",
            avatarId = review.long("image_id", 0),
            token = review.string("token"),
        )
    }

    private fun JsonObject.array(name: String): JsonArray =
        this[name] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.long(name: String, fallback: Long): Long =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: fallback

    private fun JsonObject.boolean(name: String): Boolean =
        (this[name] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: false
}

internal class BandcampReviewPageParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid Bandcamp review data", cause)
