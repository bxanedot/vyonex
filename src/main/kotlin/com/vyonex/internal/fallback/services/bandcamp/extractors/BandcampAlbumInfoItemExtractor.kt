// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.ListExtractor
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor
import com.vyonex.internal.json.JsonObject

public class BandcampAlbumInfoItemExtractor(
    private val album: JsonObject,
    private val uploaderUrl: String?,
) : PlaylistInfoItemExtractor {
    @Throws(ParsingException::class)
    override fun getName(): String = album.getString("title")
        ?.takeIf(String::isNotBlank)
        ?: throw ParsingException("Bandcamp album item is missing title")

    @Throws(ParsingException::class)
    override fun getUrl(): String = BandcampExtractorHelper.getStreamUrlFromIds(
        album.getLong("band_id"),
        album.getLong("item_id"),
        album.getString("item_type"),
    )

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(album.getLong("art_id"), true)

    override fun getUploaderName(): String = album.getString("band_name", "")

    override fun getUploaderUrl(): String? = uploaderUrl

    override fun isUploaderVerified(): Boolean = false

    override fun getStreamCount(): Long = ListExtractor.ITEM_COUNT_UNKNOWN
}
