// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.ListExtractor
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor
import com.vyonex.internal.fallback.services.bandcamp.BandcampDiscographyItem
import com.vyonex.internal.json.JsonObject

public class BandcampAlbumInfoItemExtractor private constructor(
    private val title: String?,
    private val bandName: String,
    private val bandId: Long,
    private val itemId: Long,
    private val artId: Long,
    private val itemType: String?,
    private val uploaderUrl: String?,
) : PlaylistInfoItemExtractor {
    public constructor(album: JsonObject, uploaderUrl: String?) : this(
        album.getString("title"),
        album.getString("band_name", ""),
        album.getLong("band_id"),
        album.getLong("item_id"),
        album.getLong("art_id"),
        album.getString("item_type"),
        uploaderUrl,
    )

    internal constructor(album: BandcampDiscographyItem, uploaderUrl: String?) : this(
        album.title,
        album.artist,
        album.artistId,
        album.itemId,
        album.artId,
        album.type,
        uploaderUrl,
    )

    @Throws(ParsingException::class)
    override fun getName(): String = title
        ?.takeIf(String::isNotBlank)
        ?: throw ParsingException("Bandcamp album item is missing title")

    @Throws(ParsingException::class)
    override fun getUrl(): String = BandcampExtractorHelper.getStreamUrlFromIds(
        bandId,
        itemId,
        itemType,
    )

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(artId, true)

    override fun getUploaderName(): String = bandName

    override fun getUploaderUrl(): String? = uploaderUrl

    override fun isUploaderVerified(): Boolean = false

    override fun getStreamCount(): Long = ListExtractor.ITEM_COUNT_UNKNOWN
}
