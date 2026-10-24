// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.downloader.Response
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ReCaptchaException
import com.vyonex.internal.fallback.localization.Localization
import com.vyonex.internal.json.JsonArray
import com.vyonex.internal.json.JsonObject
import com.vyonex.internal.json.JsonParser
import com.vyonex.internal.json.JsonParserException
import java.io.IOException
import java.util.Collections

public object MediaCCCParsingHelper {
    private val liveId = Regex("[A-Za-z0-9_-]+/[A-Za-z0-9_-]+")

    @JvmStatic
    public fun isLiveStreamId(id: String?): Boolean = id != null && liveId.matches(id)

    @JvmStatic
    @Throws(ExtractionException::class)
    public fun getLiveStreams(
        downloader: Downloader,
        localization: Localization,
    ): JsonArray = try {
        JsonParser.array().from(fetchLiveStreamsBody(downloader, localization))
    } catch (error: JsonParserException) {
        throw ExtractionException("Could not parse live stream data.", error)
    }

    @JvmStatic
    @Throws(ExtractionException::class)
    public fun fetchLiveStreamsBody(
        downloader: Downloader,
        localization: Localization,
    ): String {
        try {
            val response = downloader.get(LIVE_STREAMS_URL, localization)
                ?: throw ExtractionException("No live stream response received")
            if (response.responseCode() !in 200..299) {
                throw ExtractionException(
                    "Live stream request failed with HTTP ${response.responseCode()}",
                )
            }
            return response.responseBody()
        } catch (error: IOException) {
            throw ExtractionException("Could not fetch live stream data.", error)
        } catch (error: ReCaptchaException) {
            throw ExtractionException("Could not fetch live stream data.", error)
        }
    }

    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun fetchJsonObject(
        downloader: Downloader,
        url: String,
        localization: Localization?,
    ): JsonObject = try {
        JsonParser.`object`().from(fetchJsonBody(downloader, url, localization))
    } catch (error: JsonParserException) {
        throw ExtractionException("Could not parse API response from $url", error)
    }

    @JvmStatic
    @Throws(IOException::class, ExtractionException::class)
    public fun fetchJsonBody(
        downloader: Downloader,
        url: String,
        localization: Localization?,
    ): String {
        val response: Response? = if (localization == null) {
            downloader.get(url)
        } else {
            downloader.get(url, localization)
        }
        if (response == null) throw ExtractionException("No API response received from $url")
        if (response.responseCode() !in 200..299) {
            throw ExtractionException(
                "API request failed with HTTP ${response.responseCode()} for $url",
            )
        }
        return response.responseBody()
    }

    @JvmStatic
    public fun getImageListFromLogoImageUrl(url: String?): List<Image> =
        if (url.isNullOrEmpty()) {
            emptyList()
        } else {
            listOf(Image(url, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, Image.ResolutionLevel.UNKNOWN))
        }

    @JvmStatic
    public fun getThumbnailsFromStreamItem(item: JsonObject): List<Image> =
        thumbnails(item, "thumb_url", "poster_url")

    @JvmStatic
    public fun getThumbnailsFromLiveStreamItem(item: JsonObject): List<Image> =
        thumbnails(item, "thumb", "poster")

    private fun thumbnails(item: JsonObject, previewKey: String, posterKey: String): List<Image> {
        val images = mutableListOf<Image>()
        appendImage(images, item.getString(previewKey), Image.ResolutionLevel.MEDIUM)
        appendImage(images, item.getString(posterKey), Image.ResolutionLevel.HIGH)
        return Collections.unmodifiableList(images)
    }

    private fun appendImage(images: MutableList<Image>, url: String?, quality: Image.ResolutionLevel) {
        if (!url.isNullOrEmpty()) {
            images.add(Image(url, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, quality))
        }
    }

    private const val LIVE_STREAMS_URL = "https://streaming.media.ccc.de/streams/v2.json"
}
