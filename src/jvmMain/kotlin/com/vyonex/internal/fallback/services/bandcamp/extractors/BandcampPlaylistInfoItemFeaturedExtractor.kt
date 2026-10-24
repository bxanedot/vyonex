// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor
import com.vyonex.internal.fallback.services.bandcamp.BandcampFeaturedItem
import com.vyonex.internal.json.JsonObject

public class BandcampPlaylistInfoItemFeaturedExtractor private constructor(
    private val uploaderName: String,
    private val streamCount: Long,
    private val name: String,
    itemUrl: String,
    private val artworkId: Long,
) : PlaylistInfoItemExtractor {
    private val url = itemUrl
        .replaceFirst(Regex("^http://"), "https://")

    public constructor(story: JsonObject) : this(
        story.getString("band_name", ""),
        story.getLong("num_streamable_tracks", -1),
        story.getString("album_title", ""),
        story.getString("item_url", ""),
        story.getLong(if (story.isNumber("art_id")) "art_id" else "item_art_id"),
    )

    internal constructor(story: BandcampFeaturedItem) : this(
        story.artist,
        story.streamCount,
        story.title,
        story.url,
        story.artworkId,
    )

    override fun getUploaderName(): String = uploaderName

    override fun getUploaderUrl(): String? = null

    override fun isUploaderVerified(): Boolean = false

    override fun getStreamCount(): Long = streamCount

    override fun getName(): String = name

    override fun getUrl(): String = url

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(artworkId, true)
}
