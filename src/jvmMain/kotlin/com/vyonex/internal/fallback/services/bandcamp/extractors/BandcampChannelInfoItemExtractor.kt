// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor
import com.vyonex.internal.fallback.exceptions.ParsingException
import org.jsoup.nodes.Element

public class BandcampChannelInfoItemExtractor(result: Element) : ChannelInfoItemExtractor {
    private val name = text(result, ".heading")
    private val url = text(result, ".itemurl")
    private val description = text(result, ".subhead")
    private val imageUrl = result.selectFirst(".art img[src]")?.attr("src").orEmpty()

    override fun getName(): String = name

    @Throws(ParsingException::class)
    override fun getUrl(): String = url.takeIf(String::isNotBlank)
        ?: throw ParsingException("Bandcamp artist result has no URL")

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageUrl(imageUrl)

    override fun getDescription(): String = description

    override fun getSubscriberCount(): Long = -1

    override fun getStreamCount(): Long = -1

    override fun isVerified(): Boolean = false

    private companion object {
        fun text(result: Element, selector: String): String =
            result.selectFirst(".result-info $selector")?.text().orEmpty()
    }
}
