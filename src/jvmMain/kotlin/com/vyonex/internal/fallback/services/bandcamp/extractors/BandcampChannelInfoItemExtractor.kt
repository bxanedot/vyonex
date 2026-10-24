// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor
import com.vyonex.internal.fallback.exceptions.ParsingException
internal class BandcampChannelInfoItemExtractor(result: BandcampSearchItem.Artist) : ChannelInfoItemExtractor {
    private val name = result.name
    private val url = result.url
    private val description = result.description
    private val imageUrl = result.imageUrl

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
}
