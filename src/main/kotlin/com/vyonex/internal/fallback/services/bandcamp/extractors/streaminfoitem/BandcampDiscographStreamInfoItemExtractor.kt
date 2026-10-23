// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper
import com.vyonex.internal.json.JsonObject

public class BandcampDiscographStreamInfoItemExtractor(
    private val item: JsonObject,
    uploaderUrl: String?,
) : BandcampStreamInfoItemExtractor(uploaderUrl) {
    override fun getUploaderName(): String = item.getString("band_name", "")

    @Throws(ParsingException::class)
    override fun getName(): String = item.getString("title")
        ?.takeIf(String::isNotBlank)
        ?: throw ParsingException("Bandcamp track has no title")

    @Throws(ParsingException::class)
    override fun getUrl(): String = BandcampExtractorHelper.getStreamUrlFromIds(
        item.getLong("band_id"),
        item.getLong("item_id"),
        item.getString("item_type"),
    )

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(item.getLong("art_id"), true)

    override fun getDuration(): Long = -1
}
