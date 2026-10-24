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
import com.vyonex.internal.json.JsonWriter
import kotlinx.serialization.json.JsonObject as JsonTreeObject
import java.net.URI
import java.util.Collections

public class PeertubeParsingHelper private constructor() {
    public companion object {
        public const val START_KEY = PeertubePagination.START_KEY
        public const val COUNT_KEY = "count"
        public const val ITEMS_PER_PAGE = PeertubePagination.ITEMS_PER_PAGE

        @JvmStatic
        @Throws(ContentNotAvailableException::class)
        public fun validate(response: JsonObject) {
            response.getString("error")?.takeIf { it.isNotBlank() }
                ?.let { throw ContentNotAvailableException(it) }
        }

        @JvmStatic
        @Throws(ExtractionException::class)
        public fun readApiObject(response: Response?, resource: String): JsonObject {
            val body = readApiBody(response, resource)
            return try {
                JsonParser.`object`().from(body)
            } catch (error: JsonParserException) {
                throw ParsingException("Invalid PeerTube $resource response", error)
            }
        }

        @JvmStatic
        @Throws(ExtractionException::class)
        public fun readApiBody(response: Response?, resource: String): String {
            if (response == null) throw ExtractionException("PeerTube returned no $resource")
            val status = response.responseCode()
            if (status !in 200..299) {
                throw ExtractionException("PeerTube $resource request failed with HTTP $status")
            }
            val body = response.responseBody()
            if (body.isBlank()) throw ExtractionException("PeerTube returned no $resource")
            return body
        }

        @JvmStatic
        public fun getNextPage(previousUrl: String, total: Long): Page? =
            PeertubePagination.nextPageUrl(previousUrl, total)?.let(::Page)

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
            PeertubeFieldReader.stringAt(item, path)
                ?: throw ParsingException("Missing string field: $path")

        @JvmStatic
        public fun optionalString(item: JsonObject, path: String): String? =
            PeertubeFieldReader.stringAt(item, path)

        @JvmStatic
        @Throws(ParsingException::class)
        public fun requiredNumber(item: JsonObject, path: String): Number =
            PeertubeFieldReader.numberAt(item, path)
                ?: throw ParsingException("Missing numeric field: $path")

        @JvmStatic
        public fun getAvatarsFromOwnerAccountOrVideoChannelObject(
            baseUrl: String,
            item: JsonObject,
        ): List<Image> = profileImages(baseUrl, item, "avatars", "avatar")

        internal fun getAvatarsFromOwnerAccountOrVideoChannelObject(
            baseUrl: String,
            item: JsonTreeObject,
        ): List<Image> = toImages(
            PeertubeImageParser.profileImages(item, "avatars", "avatar"), baseUrl,
        )

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
        ): List<Image> = toImages(
            PeertubeImageParser.thumbnails(thumbnailPath, previewPath), baseUrl,
        )

        @Suppress("UNCHECKED_CAST")
        private fun commit(collector: InfoItemsCollector<*, *>, extractor: InfoItemExtractor) {
            (collector as InfoItemsCollector<InfoItem, InfoItemExtractor>).commit(extractor)
        }

        private fun profileImages(
            baseUrl: String,
            item: JsonObject,
            listKey: String,
            fallbackKey: String,
        ): List<Image> = toImages(
            PeertubeImageParser.profileImages(JsonWriter.string(item), listKey, fallbackKey),
            baseUrl,
        )

        internal fun toImages(images: List<PeertubeImageReference>, baseUrl: String): List<Image> =
            Collections.unmodifiableList(images.map { image ->
                Image(
                    resolve(baseUrl, image.path),
                    Image.HEIGHT_UNKNOWN,
                    image.width,
                    when (image.size) {
                        PeertubeImageSize.LOW -> ResolutionLevel.LOW
                        PeertubeImageSize.MEDIUM -> ResolutionLevel.MEDIUM
                        PeertubeImageSize.UNKNOWN -> ResolutionLevel.UNKNOWN
                    },
                )
            })

        private fun resolve(baseUrl: String, path: String): String = try {
            val base = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
            URI.create(base).resolve(path).toString()
        } catch (_: IllegalArgumentException) {
            baseUrl + if (path.startsWith('/')) path else "/$path"
        }

    }
}
