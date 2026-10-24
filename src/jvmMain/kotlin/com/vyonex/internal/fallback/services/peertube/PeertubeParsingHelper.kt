// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.Image.ResolutionLevel
import com.vyonex.internal.fallback.InfoItem
import com.vyonex.internal.fallback.InfoItemExtractor
import com.vyonex.internal.fallback.InfoItemsCollector
import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.downloader.Response
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeChannelInfoItemExtractor
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubePlaylistInfoItemExtractor
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeSepiaStreamInfoItemExtractor
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeStreamInfoItemExtractor
import com.vyonex.internal.json.JsonArray
import com.vyonex.internal.json.JsonObject
import com.vyonex.internal.json.JsonParser
import com.vyonex.internal.json.JsonParserException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Collections

public class PeertubeParsingHelper private constructor() {
    public companion object {
        public const val START_KEY = "start"
        public const val COUNT_KEY = "count"
        public const val ITEMS_PER_PAGE = 12

        @JvmStatic
        @Throws(ContentNotAvailableException::class)
        public fun validate(response: JsonObject) {
            response.getString("error")?.takeIf { it.isNotBlank() }
                ?.let { throw ContentNotAvailableException(it) }
        }

        @JvmStatic
        @Throws(ExtractionException::class)
        public fun readApiObject(response: Response?, resource: String): JsonObject {
            if (response == null) throw ExtractionException("PeerTube returned no $resource")
            val status = response.responseCode()
            if (status !in 200..299) {
                throw ExtractionException("PeerTube $resource request failed with HTTP $status")
            }
            val body = response.responseBody()
            if (body.isBlank()) throw ExtractionException("PeerTube returned no $resource")
            return try {
                JsonParser.`object`().from(body)
            } catch (error: JsonParserException) {
                throw ParsingException("Invalid PeerTube $resource response", error)
            }
        }

        @JvmStatic
        public fun getNextPage(previousUrl: String, total: Long): Page? {
            val queryStart = previousUrl.indexOf('?')
            if (queryStart < 0) return null
            val queryEnd = previousUrl.indexOf('#', queryStart).let {
                if (it < 0) previousUrl.length else it
            }

            var start = queryStart + 1
            while (start < queryEnd) {
                val separator = previousUrl.indexOf('&', start).let {
                    if (it < 0 || it > queryEnd) queryEnd else it
                }
                val equals = previousUrl.indexOf('=', start).takeIf { it in start until separator }
                val nameEnd = equals ?: separator
                if (previousUrl.substring(start, nameEnd) == START_KEY) {
                    val valueStart = equals?.plus(1) ?: return null
                    val current = try {
                        URLDecoder.decode(previousUrl.substring(valueStart, separator), StandardCharsets.UTF_8)
                            .toLong()
                    } catch (_: IllegalArgumentException) {
                        return null
                    }
                    if (current < 0) return null
                    val next = try {
                        Math.addExact(current, ITEMS_PER_PAGE.toLong())
                    } catch (_: ArithmeticException) {
                        return null
                    }
                    if (next >= total) return null
                    return Page(previousUrl.substring(0, valueStart) + next
                        + previousUrl.substring(separator))
                }
                start = separator + 1
            }
            return null
        }

        @JvmStatic
        @Throws(ParsingException::class)
        public fun collectItemsFrom(
            collector: InfoItemsCollector<*, *>,
            response: JsonObject,
            baseUrl: String,
        ) = collectItemsFrom(collector, response, baseUrl, false)

        @JvmStatic
        @Throws(ParsingException::class)
        public fun collectItemsFrom(
            collector: InfoItemsCollector<*, *>,
            response: JsonObject,
            baseUrl: String,
            sepia: Boolean,
        ) {
            val data = response["data"] as? JsonArray
                ?: throw ParsingException("Response data is not an array")

            for (value in data) {
                var item = value as? JsonObject ?: continue
                item = (item["video"] as? JsonObject) ?: item
                val extractor = when {
                    sepia -> PeertubeSepiaStreamInfoItemExtractor(item, baseUrl)
                    item.has("videosLength") -> PeertubePlaylistInfoItemExtractor(item, baseUrl)
                    item.has("followersCount") -> PeertubeChannelInfoItemExtractor(item, baseUrl)
                    else -> PeertubeStreamInfoItemExtractor(item, baseUrl)
                }
                commit(collector, extractor)
            }
        }

        @JvmStatic
        @Throws(ParsingException::class)
        public fun requiredString(item: JsonObject, path: String): String =
            valueAt(item, path) as? String
                ?: throw ParsingException("Missing string field: $path")

        @JvmStatic
        public fun optionalString(item: JsonObject, path: String): String? =
            valueAt(item, path) as? String

        @JvmStatic
        @Throws(ParsingException::class)
        public fun requiredNumber(item: JsonObject, path: String): Number =
            valueAt(item, path) as? Number
                ?: throw ParsingException("Missing numeric field: $path")

        @JvmStatic
        public fun getAvatarsFromOwnerAccountOrVideoChannelObject(
            baseUrl: String,
            item: JsonObject,
        ): List<Image> = profileImages(baseUrl, item, "avatars", "avatar")

        @JvmStatic
        public fun getBannersFromAccountOrVideoChannelObject(
            baseUrl: String,
            item: JsonObject,
        ): List<Image> = profileImages(baseUrl, item, "banners", "banner")

        @JvmStatic
        public fun getThumbnailsFromPlaylistOrVideoItem(
            baseUrl: String,
            item: JsonObject,
        ): List<Image> = getThumbnailsFromPlaylistOrVideoItem(
            baseUrl, item.getString("thumbnailPath"), item.getString("previewPath"),
        )

        @JvmStatic
        public fun getThumbnailsFromPlaylistOrVideoItem(
            baseUrl: String,
            thumbnailPath: String?,
            previewPath: String?,
        ): List<Image> {
            val images = ArrayList<Image>(2)
            addThumbnail(images, baseUrl, thumbnailPath, ResolutionLevel.LOW)
            addThumbnail(images, baseUrl, previewPath, ResolutionLevel.MEDIUM)
            return Collections.unmodifiableList(images)
        }

        @Suppress("UNCHECKED_CAST")
        private fun commit(collector: InfoItemsCollector<*, *>, extractor: InfoItemExtractor) {
            (collector as InfoItemsCollector<InfoItem, InfoItemExtractor>).commit(extractor)
        }

        private fun profileImages(
            baseUrl: String,
            item: JsonObject,
            listKey: String,
            fallbackKey: String,
        ): List<Image> {
            val values = item.getArray(listKey)
            val images = ArrayList<Image>(if (values.isEmpty()) 1 else values.size)
            if (values.isEmpty()) {
                addProfileImage(images, baseUrl, item.getObject(fallbackKey))
            } else {
                values.filterIsInstance<JsonObject>().forEach { addProfileImage(images, baseUrl, it) }
            }
            return Collections.unmodifiableList(images)
        }

        private fun addProfileImage(images: MutableList<Image>, baseUrl: String, value: JsonObject) {
            val path = value.getString("path")
            if (!path.isNullOrBlank()) {
                images.add(
                    Image(
                        resolve(baseUrl, path), Image.HEIGHT_UNKNOWN,
                        value.getInt("width", Image.WIDTH_UNKNOWN), ResolutionLevel.UNKNOWN,
                    ),
                )
            }
        }

        private fun addThumbnail(
            images: MutableList<Image>,
            baseUrl: String,
            path: String?,
            resolution: ResolutionLevel,
        ) {
            if (!path.isNullOrBlank()) {
                images.add(Image(resolve(baseUrl, path), Image.HEIGHT_UNKNOWN,
                    Image.WIDTH_UNKNOWN, resolution))
            }
        }

        private fun resolve(baseUrl: String, path: String): String = try {
            val base = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
            URI.create(base).resolve(path).toString()
        } catch (_: IllegalArgumentException) {
            baseUrl + if (path.startsWith('/')) path else "/$path"
        }

        private fun valueAt(item: JsonObject, path: String): Any? {
            var value: Any? = item
            for (key in path.split('.')) {
                value = (value as? JsonObject)?.get(key) ?: return null
            }
            return value
        }
    }
}
