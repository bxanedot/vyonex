// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.channel.ChannelExtractor
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.services.peertube.PeertubeChannelPageParseException
import com.vyonex.internal.fallback.services.peertube.PeertubeChannelPageParser
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory
import java.io.IOException

public class PeertubeChannelExtractor @Throws(ParsingException::class) constructor(
    service: StreamingService,
    link: ListLinkHandler,
) : ChannelExtractor(service, link) {
    private val baseUrl = getBaseUrl()
    private var apiId: String? = null
    private var displayName: String? = null
    private var description: String? = null
    private var subscriberCount = 0L
    private var ownerName: String? = null
    private var ownerUrl: String? = null
    private var avatars: List<Image> = emptyList()
    private var banners: List<Image> = emptyList()
    private var ownerAvatars: List<Image> = emptyList()

    override fun getAvatars(): List<Image> = avatars

    override fun getBanners(): List<Image> = banners

    override fun getFeedUrl(): String = "$baseUrl/feeds/videos.xml?videoChannelId=$apiId"

    override fun getSubscriberCount(): Long = subscriberCount

    override fun getDescription(): String? = description

    @Throws(ParsingException::class)
    override fun getParentChannelName(): String = required(ownerName, "ownerAccount.name")

    @Throws(ParsingException::class)
    override fun getParentChannelUrl(): String = required(ownerUrl, "ownerAccount.url")

    override fun getParentChannelAvatars(): List<Image> = ownerAvatars

    override fun isVerified(): Boolean = false

    @Throws(ParsingException::class)
    override fun getTabs(): List<ListLinkHandler> {
        val tabs = PeertubeChannelTabLinkHandlerFactory.getInstance()
        return listOf(
            tabs.fromQuery(getId(), listOf(ChannelTabs.VIDEOS), "", baseUrl),
            tabs.fromQuery(getId(), listOf(ChannelTabs.PLAYLISTS), "", baseUrl),
        )
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val response = PeertubeParsingHelper.readApiBody(
            downloader.get("$baseUrl$API_ENDPOINT${getId()}"), "channel",
        )
        val channel = try {
            PeertubeChannelPageParser.parse(response)
        } catch (error: PeertubeChannelPageParseException) {
            throw ParsingException("Invalid PeerTube channel response", error)
        }
        channel.error?.let { throw ContentNotAvailableException(it) }
        apiId = channel.id
        displayName = channel.displayName
        description = channel.description
        subscriberCount = channel.followerCount
        ownerName = channel.ownerName
        ownerUrl = channel.ownerUrl
        avatars = PeertubeParsingHelper.toImages(channel.avatars, baseUrl)
        banners = PeertubeParsingHelper.toImages(channel.banners, baseUrl)
        ownerAvatars = PeertubeParsingHelper.toImages(channel.ownerAvatars, baseUrl)
    }

    @Throws(ParsingException::class)
    override fun getName(): String = required(displayName, "displayName")

    private fun required(value: String?, field: String): String = value
        ?: throw ParsingException("Missing string field: $field")

    private companion object {
        const val API_ENDPOINT = "/api/v1/"
    }
}
