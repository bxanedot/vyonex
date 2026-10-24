// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper
import org.jsoup.nodes.Element
import java.util.Locale

public class BandcampSearchStreamInfoItemExtractor(
    result: Element,
    uploaderUrl: String?,
) : BandcampStreamInfoItemExtractor(uploaderUrl) {
    private val uploaderName: String
    private val name = text(result, ".heading")
    private val url = text(result, ".itemurl")
    private val imageUrl = result.selectFirst(".art img[src]")?.attr("src").orEmpty()

    init {
        val subtitle = text(result, ".subhead")
        val separator = subtitle.lowercase(Locale.ROOT).lastIndexOf(" by ")
        uploaderName = if (separator < 0) subtitle else subtitle.substring(separator + 4).trim()
    }

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

    private companion object {
        fun text(result: Element, selector: String): String =
            result.selectFirst(".result-info $selector")?.text().orEmpty()
    }
}
