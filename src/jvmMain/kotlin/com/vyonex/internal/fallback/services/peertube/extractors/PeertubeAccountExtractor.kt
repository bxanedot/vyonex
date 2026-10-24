// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.channel.ChannelExtractor
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.downloader.Response
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory
import com.vyonex.internal.json.JsonArray
import com.vyonex.internal.json.JsonObject
import java.io.IOException

public class PeertubeAccountExtractor @Throws(ParsingException::class) constructor(
    service: StreamingService,
    link: ListLinkHandler,
) : ChannelExtractor(service, link) {
    private val baseUrl = getBaseUrl()
    private var apiId: String? = null
    private var displayName: String? = null
    private var description: String? = null
    private var accountFollowers = 0L
    private var avatars: List<Image> = emptyList()
    private var banners: List<Image> = emptyList()

    override fun getAvatars(): List<Image> = avatars

    override fun getBanners(): List<Image> = banners

    override fun getFeedUrl(): String = "$baseUrl/feeds/videos.xml?accountId=$apiId"

    override fun getDescription(): String? = description

    override fun getParentChannelName(): String = ""

    override fun getParentChannelUrl(): String = ""

    override fun getParentChannelAvatars(): List<Image> = emptyList()

    override fun isVerified(): Boolean = false

    @Throws(ParsingException::class)
    override fun getSubscriberCount(): Long {
        var followers = accountFollowers
        val channelListUrl = "$baseUrl$API${getId()}/video-channels"
        try {
            val result = PeertubeParsingHelper.readApiObject(
                getDownloader().get(channelListUrl), "account channel data",
            )
            val channels: JsonArray = result.getArray("data")
            for (value in channels) {
                if (value is JsonObject) followers += value.getLong("followersCount")
            }
        } catch (_: Exception) {
            // The account total remains useful when its optional channel list cannot be read.
        }
        return followers
    }

    @Throws(ParsingException::class)
    override fun getTabs(): List<ListLinkHandler> {
        val tabs = PeertubeChannelTabLinkHandlerFactory.getInstance()
        return listOf(
            tabs.fromQuery(getId(), listOf(ChannelTabs.VIDEOS), "", baseUrl),
            tabs.fromQuery(getId(), listOf(ChannelTabs.CHANNELS), "", baseUrl),
        )
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val account = PeertubeParsingHelper.readApiObject(
            downloader.get("$baseUrl$API${getId()}"), "account",
        )
        PeertubeParsingHelper.validate(account)
        apiId = account["id"].toString()
        displayName = account.getString("displayName")
        description = account.getString("description")
        accountFollowers = account.getLong("followersCount")
        avatars = PeertubeParsingHelper
            .getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, account)
        banners = PeertubeParsingHelper.getBannersFromAccountOrVideoChannelObject(baseUrl, account)
    }

    @Throws(ParsingException::class)
    override fun getName(): String = displayName
        ?: throw ParsingException("Missing string field: displayName")

    private companion object {
        const val API = "/api/v1/"
    }
}
