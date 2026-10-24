// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor
import org.jsoup.nodes.Element

public class BandcampRelatedPlaylistInfoItemExtractor(album: Element) : PlaylistInfoItemExtractor {
    private val name = album.selectFirst(".release-title")?.text().orEmpty()
    private val url = album.selectFirst(".album-link[href]")?.absUrl("href")
        ?.replaceFirst(Regex("^http://"), "https://")
        .orEmpty()
    private val imageUrl = album.selectFirst(".album-art[src]")?.attr("src").orEmpty()
    private val uploaderName = album.selectFirst(".by-artist")?.text()
        ?.replaceFirst(Regex("(?i)^by\\s+"), "")
        .orEmpty()

    override fun getName(): String = name

    override fun getUrl(): String = url

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageUrl(imageUrl)

    override fun getUploaderName(): String = uploaderName

    override fun getUploaderUrl(): String? = null

    override fun isUploaderVerified(): Boolean = false

    override fun getStreamCount(): Long = -1
}
