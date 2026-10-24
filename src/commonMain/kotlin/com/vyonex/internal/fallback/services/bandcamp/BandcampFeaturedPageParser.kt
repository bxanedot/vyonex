// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.bandcamp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal data class BandcampFeaturedItem(
    val title: String,
    val artist: String,
    val url: String,
    val streamCount: Long,
    val artworkId: Long,
)

internal data class BandcampFeaturedPagePayload(
    val items: List<BandcampFeaturedItem>,
    val nextStoryGroup: String?,
)

internal object BandcampFeaturedPageParser {
    fun parseInitial(source: String): BandcampFeaturedPagePayload {
        val root = parseObject(source)
        val stories = ((root["feed_content"] as? JsonObject)?.get("stories") as? JsonObject)
            ?.get("featured") as? JsonArray
        return parseStories(stories)
    }

    fun parseOlder(source: String): BandcampFeaturedPagePayload {
        val root = parseObject(source)
        val stories = (root["stories"] as? JsonObject)?.get("featured") as? JsonArray
        return parseStories(stories)
    }

    private fun parseObject(source: String): JsonObject = try {
        Json.parseToJsonElement(source) as? JsonObject ?: throw BandcampFeaturedPageParseException()
    } catch (error: SerializationException) {
        throw BandcampFeaturedPageParseException(error)
    }

    private fun parseStories(stories: JsonArray?): BandcampFeaturedPagePayload {
        val data = stories ?: throw BandcampFeaturedStoriesMissingException()
        val storyObjects = data.filterIsInstance<JsonObject>()
        val items = storyObjects.mapNotNull { story ->
            val title = story.string("album_title") ?: return@mapNotNull null
            BandcampFeaturedItem(
                title = title,
                artist = story.string("band_name") ?: "",
                url = story.string("item_url") ?: "",
                streamCount = story.long("num_streamable_tracks") ?: -1,
                artworkId = story.long("art_id")
                    ?: story.long("item_art_id")
                    ?: 0,
            )
        }
        val last = storyObjects.lastOrNull()
        val type = last?.string("story_type")
        val date = last?.long("story_date")
        val id = last?.long("ntid")
        val nextStoryGroup = if (type != null && date != null && id != null) {
            "featured:$date:$type:$id"
        } else {
            null
        }
        return BandcampFeaturedPagePayload(items, nextStoryGroup)
    }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content

    private fun JsonObject.long(name: String): Long? =
        (this[name] as? JsonPrimitive)?.takeUnless(JsonPrimitive::isString)
            ?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }
}

internal class BandcampFeaturedPageParseException(cause: Throwable? = null) :
    IllegalArgumentException("Invalid Bandcamp featured data", cause)

internal class BandcampFeaturedStoriesMissingException :
    IllegalArgumentException("Bandcamp featured data has no stories")
