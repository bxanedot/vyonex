// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.ServiceList
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.localization.DateWrapper
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor
import com.vyonex.internal.fallback.stream.StreamType
import com.vyonex.internal.json.JsonObject

public open class PeertubeStreamInfoItemExtractor(
    item: JsonObject,
    private var baseUrl: String,
) : StreamInfoItemExtractor {
    private val uuid = item.getString("uuid")
    private val name = item.getString("name")
    protected val embedUrl = item.getString("embedUrl")
    private val account: JsonObject = item.getObject("account")
    private val thumbnailPath = item.getString("thumbnailPath")
    private val previewPath = item.getString("previewPath")
    private val publishedAt = item.getString("publishedAt")
    private val views = item.getLong("views", -1)
    private val duration = item.getLong("duration", -1)
    private val live = item.getBoolean("isLive")

    @Throws(ParsingException::class)
    override fun getUrl(): String = ServiceList.PeerTube.getStreamLHFactory()
        .fromId(required(uuid, "uuid"), baseUrl).url

    override fun getThumbnails(): List<Image> =
        PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem(
            baseUrl, thumbnailPath, previewPath,
        )

    @Throws(ParsingException::class)
    override fun getName(): String = required(name, "name")

    override fun isAd(): Boolean = false

    override fun getViewCount(): Long = views

    @Throws(ParsingException::class)
    override fun getUploaderUrl(): String {
        val accountName = required(account.getString("name"), "account.name")
        val host = required(account.getString("host"), "account.host")
        return ServiceList.PeerTube.getChannelLHFactory()
            .fromId("accounts/$accountName@$host", baseUrl).url
    }

    override fun getUploaderAvatars(): List<Image> = PeertubeParsingHelper
        .getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, account)

    override fun isUploaderVerified(): Boolean = false

    @Throws(ParsingException::class)
    override fun getUploaderName(): String = required(account.getString("displayName"), "account.displayName")

    override fun getTextualUploadDate(): String? = publishedAt

    @Throws(ParsingException::class)
    override fun getUploadDate(): DateWrapper? = publishedAt?.let(DateWrapper::fromInstant)

    override fun getStreamType(): StreamType =
        if (live) StreamType.LIVE_STREAM else StreamType.VIDEO_STREAM

    override fun getDuration(): Long = duration

    protected fun setBaseUrl(value: String) {
        // The URL remains tied to this extractor's instance when it is hosted remotely.
        this.baseUrl = value
    }

    private fun required(value: String?, field: String): String = value
        ?: throw ParsingException("Missing string field: $field")
}
