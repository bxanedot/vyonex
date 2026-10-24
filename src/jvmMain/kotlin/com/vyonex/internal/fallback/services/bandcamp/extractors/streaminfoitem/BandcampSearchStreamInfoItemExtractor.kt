// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampSearchItem

internal class BandcampSearchStreamInfoItemExtractor(
    result: BandcampSearchItem.Track,
    uploaderUrl: String?,
) : BandcampStreamInfoItemExtractor(uploaderUrl) {
    private val uploaderName = result.uploaderName
    private val name = result.name
    private val url = result.url
    private val imageUrl = result.imageUrl

    override fun getUploaderName(): String = uploaderName

    @Throws(ParsingException::class)
    override fun getName(): String = required(name, ".heading")

    @Throws(ParsingException::class)
    override fun getUrl(): String = required(url, ".itemurl")

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageUrl(imageUrl)

    override fun getDuration(): Long = -1

    private fun required(value: String, selector: String): String =
        value.takeIf(String::isNotBlank)
            ?: throw ParsingException("Bandcamp search result is missing $selector")

}
