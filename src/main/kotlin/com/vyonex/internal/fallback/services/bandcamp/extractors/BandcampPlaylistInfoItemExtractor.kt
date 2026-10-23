// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor
import org.jsoup.nodes.Element
import java.util.Locale

public class BandcampPlaylistInfoItemExtractor(result: Element) : PlaylistInfoItemExtractor {
    private val subtitle = text(result, ".subhead")
    private val artistSeparator = subtitle.lowercase(Locale.ROOT).lastIndexOf(" by ")
    private val uploaderName = if (artistSeparator < 0) subtitle
        else subtitle.substring(artistSeparator + 4).trim()
    private val streamCount = LEADING_COUNT.find(text(result, ".length"))
        ?.groupValues?.get(1)?.toLongOrNull() ?: -1L
    private val name = text(result, ".heading")
    private val url = text(result, ".itemurl")
    private val imageUrl = result.selectFirst(".art img[src]")?.attr("src").orEmpty()

    override fun getUploaderName(): String = uploaderName

    override fun getUploaderUrl(): String? = null

    override fun isUploaderVerified(): Boolean = false

    override fun getStreamCount(): Long = streamCount

    override fun getName(): String = name

    override fun getUrl(): String = url

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageUrl(imageUrl)

    private companion object {
        val LEADING_COUNT = Regex("^\\s*(\\d+)")

        fun text(result: Element, selector: String): String =
            result.selectFirst(".result-info $selector")?.text().orEmpty()
    }
}
