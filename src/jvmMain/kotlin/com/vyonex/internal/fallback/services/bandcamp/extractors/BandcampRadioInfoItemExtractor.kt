// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.localization.DateWrapper
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor
import com.vyonex.internal.fallback.stream.StreamType
import com.vyonex.internal.fallback.services.bandcamp.BandcampRadioShow
import com.vyonex.internal.json.JsonObject

public class BandcampRadioInfoItemExtractor private constructor(
    private val id: Long,
    private val subtitle: String?,
    private val title: String,
    private val description: String?,
    private val date: String?,
    private val imageId: Long,
) : StreamInfoItemExtractor {
    public constructor(show: JsonObject) : this(
        show.getLong("id"),
        show.getString("subtitle"),
        show.getString("title", ""),
        show.getString("desc"),
        show.getString("date"),
        show.getLong("image_id"),
    )

    internal constructor(show: BandcampRadioShow) : this(
        show.id,
        show.subtitle,
        show.title,
        show.description,
        show.date,
        show.imageId,
    )

    override fun getDuration(): Long = 0

    override fun getShortDescription(): String? = description

    override fun getTextualUploadDate(): String? = date

    @Throws(ParsingException::class)
    override fun getUploadDate(): DateWrapper? = date
        ?.takeIf(String::isNotBlank)
        ?.let(BandcampExtractorHelper::parseDate)

    @Throws(ParsingException::class)
    override fun getName(): String = subtitle
        ?.takeIf(String::isNotBlank)
        ?: throw ParsingException("Bandcamp radio item is missing subtitle")

    override fun getUrl(): String {
        return "${BandcampExtractorHelper.BASE_URL}/?show=$id"
    }

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(imageId, false)

    override fun getStreamType(): StreamType = StreamType.AUDIO_STREAM

    override fun getViewCount(): Long = -1

    override fun getUploaderName(): String = title

    override fun getUploaderUrl(): String = ""

    override fun isUploaderVerified(): Boolean = false

    override fun isAd(): Boolean = false
}
