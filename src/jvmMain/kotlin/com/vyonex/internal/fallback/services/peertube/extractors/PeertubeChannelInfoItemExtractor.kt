// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.ListExtractor
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
import com.vyonex.internal.json.JsonObject

public class PeertubeChannelInfoItemExtractor(item: JsonObject, baseUrl: String) :
    ChannelInfoItemExtractor {
    private val name = item.getString("displayName")
    private val url = item.getString("url")
    private val thumbnails: List<Image> =
        PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, item)
    private val description = item.getString("description")
    private val subscriberCount = item.getLong("followersCount", -1)

    @Throws(ParsingException::class)
    override fun getName(): String = required(name, "displayName")

    @Throws(ParsingException::class)
    override fun getUrl(): String = required(url, "url")

    override fun getThumbnails(): List<Image> = thumbnails

    override fun getDescription(): String? = description

    override fun getSubscriberCount(): Long = subscriberCount

    override fun getStreamCount(): Long = ListExtractor.ITEM_COUNT_UNKNOWN

    override fun isVerified(): Boolean = false

    private fun required(value: String?, field: String): String = value
        ?: throw ParsingException("Missing string field: $field")
}
