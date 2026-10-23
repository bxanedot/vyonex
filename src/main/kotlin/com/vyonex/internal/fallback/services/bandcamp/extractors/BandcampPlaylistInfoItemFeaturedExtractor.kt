// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor
import com.vyonex.internal.json.JsonObject

public class BandcampPlaylistInfoItemFeaturedExtractor(story: JsonObject) : PlaylistInfoItemExtractor {
    private val uploaderName = story.getString("band_name", "")
    private val streamCount = story.getLong("num_streamable_tracks", -1)
    private val name = story.getString("album_title", "")
    private val url = story.getString("item_url", "")
        .replaceFirst(Regex("^http://"), "https://")
    private val artworkId = story.getLong(if (story.isNumber("art_id")) "art_id" else "item_art_id")

    override fun getUploaderName(): String = uploaderName

    override fun getUploaderUrl(): String? = null

    override fun isUploaderVerified(): Boolean = false

    override fun getStreamCount(): Long = streamCount

    override fun getName(): String = name

    override fun getUrl(): String = url

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(artworkId, true)
}
