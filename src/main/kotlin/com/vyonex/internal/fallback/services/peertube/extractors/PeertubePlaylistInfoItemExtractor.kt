// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
import com.vyonex.internal.fallback.stream.Description
import com.vyonex.internal.json.JsonObject

public class PeertubePlaylistInfoItemExtractor(item: JsonObject, baseUrl: String) :
    PlaylistInfoItemExtractor {
    private val displayName = item.getString("displayName")
    private val url = item.getString("url")
    private val uploader = item.getObject("uploader")
    private val uploaderName = uploader.getString("displayName")
    private val uploaderUrl = uploader.getString("url")
    private val description = item.getString("description")
    private val streamCount = item.getLong("videosLength", -1)
    private val thumbnails: List<Image> =
        PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem(baseUrl, item)

    @Throws(ParsingException::class)
    override fun getName(): String = required(displayName, "displayName")

    @Throws(ParsingException::class)
    override fun getUrl(): String = required(url, "url")

    override fun getThumbnails(): List<Image> = thumbnails

    @Throws(ParsingException::class)
    override fun getUploaderName(): String = required(uploaderName, "uploader.displayName")

    @Throws(ParsingException::class)
    override fun getUploaderUrl(): String = required(uploaderUrl, "uploader.url")

    override fun isUploaderVerified(): Boolean = false

    override fun getStreamCount(): Long = streamCount

    override fun getDescription(): Description = if (description.isNullOrEmpty()) {
        Description.EMPTY_DESCRIPTION
    } else {
        Description(description, Description.PLAIN_TEXT)
    }

    private fun required(value: String?, field: String): String = value
        ?: throw ParsingException("Missing string field: $field")
}
