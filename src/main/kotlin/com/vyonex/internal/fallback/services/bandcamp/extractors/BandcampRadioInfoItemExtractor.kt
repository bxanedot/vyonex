// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.localization.DateWrapper
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor
import com.vyonex.internal.fallback.stream.StreamType
import com.vyonex.internal.json.JsonObject

public class BandcampRadioInfoItemExtractor(private val show: JsonObject) : StreamInfoItemExtractor {
    override fun getDuration(): Long = 0

    override fun getShortDescription(): String? = show.getString("desc")

    override fun getTextualUploadDate(): String? = show.getString("date")

    @Throws(ParsingException::class)
    override fun getUploadDate(): DateWrapper? = show.getString("date")
        ?.takeIf(String::isNotBlank)
        ?.let(BandcampExtractorHelper::parseDate)

    @Throws(ParsingException::class)
    override fun getName(): String = show.getString("subtitle")
        ?.takeIf(String::isNotBlank)
        ?: throw ParsingException("Bandcamp radio item is missing subtitle")

    override fun getUrl(): String {
        val id = show.getLong("id")
        return "${BandcampExtractorHelper.BASE_URL}/?show=$id"
    }

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(show.getLong("image_id"), false)

    override fun getStreamType(): StreamType = StreamType.AUDIO_STREAM

    override fun getViewCount(): Long = -1

    override fun getUploaderName(): String = show.getString("title", "")

    override fun getUploaderUrl(): String = ""

    override fun isUploaderVerified(): Boolean = false

    override fun isAd(): Boolean = false
}
