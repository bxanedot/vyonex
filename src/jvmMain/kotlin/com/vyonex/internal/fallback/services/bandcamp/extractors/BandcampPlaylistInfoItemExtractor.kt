// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor
internal class BandcampPlaylistInfoItemExtractor(result: BandcampSearchItem.Album) : PlaylistInfoItemExtractor {
    private val uploaderName = result.uploaderName
    private val streamCount = result.trackCount
    private val name = result.name
    private val url = result.url
    private val imageUrl = result.imageUrl

    override fun getUploaderName(): String = uploaderName

    override fun getUploaderUrl(): String? = null

    override fun isUploaderVerified(): Boolean = false

    override fun getStreamCount(): Long = streamCount

    override fun getName(): String = name

    override fun getUrl(): String = url

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageUrl(imageUrl)
}
