// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.services.bandcamp.BandcampDiscographyItem
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper
import com.vyonex.internal.json.JsonObject

public class BandcampDiscographStreamInfoItemExtractor private constructor(
    private val title: String?,
    private val bandName: String,
    private val bandId: Long,
    private val itemId: Long,
    private val artId: Long,
    private val itemType: String?,
    uploaderUrl: String?,
) : BandcampStreamInfoItemExtractor(uploaderUrl) {
    public constructor(item: JsonObject, uploaderUrl: String?) : this(
        item.getString("title"),
        item.getString("band_name", ""),
        item.getLong("band_id"),
        item.getLong("item_id"),
        item.getLong("art_id"),
        item.getString("item_type"),
        uploaderUrl,
    )

    internal constructor(item: BandcampDiscographyItem, uploaderUrl: String?) : this(
        item.title,
        item.artist,
        item.artistId,
        item.itemId,
        item.artId,
        item.type,
        uploaderUrl,
    )

    override fun getUploaderName(): String = bandName

    @Throws(ParsingException::class)
    override fun getName(): String = title
        ?.takeIf(String::isNotBlank)
        ?: throw ParsingException("Bandcamp track has no title")

    @Throws(ParsingException::class)
    override fun getUrl(): String = BandcampExtractorHelper.getStreamUrlFromIds(
        bandId,
        itemId,
        itemType,
    )

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(artId, true)

    override fun getDuration(): Long = -1
}
