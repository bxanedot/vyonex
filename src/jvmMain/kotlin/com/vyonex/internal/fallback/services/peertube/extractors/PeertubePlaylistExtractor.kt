// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.playlist.PlaylistExtractor
import com.vyonex.internal.fallback.services.peertube.PeertubePlaylistPageParseException
import com.vyonex.internal.fallback.services.peertube.PeertubePlaylistPageParser
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
import com.vyonex.internal.fallback.stream.Description
import com.vyonex.internal.fallback.stream.StreamInfoItem
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector
import java.io.IOException

public class PeertubePlaylistExtractor(
    service: StreamingService,
    handler: ListLinkHandler,
) : PlaylistExtractor(service, handler) {
    private var playlist: Playlist? = null

    override fun getThumbnails(): List<Image> = state().thumbnails

    override fun getUploaderUrl(): String? = state().ownerUrl

    override fun getUploaderName(): String? = state().ownerName

    override fun getUploaderAvatars(): List<Image> = state().ownerAvatars

    override fun isUploaderVerified(): Boolean = false

    override fun getStreamCount(): Long = state().videoCount

    override fun getDescription(): Description {
        val text = state().description
        return if (text.isNullOrEmpty()) Description.EMPTY_DESCRIPTION
        else Description(text, Description.PLAIN_TEXT)
    }

    override fun getSubChannelName(): String = state().channelName.orEmpty()

    override fun getSubChannelUrl(): String = state().channelUrl.orEmpty()

    override fun getSubChannelAvatars(): List<Image> = state().channelAvatars

    @Throws(IOException::class, ExtractionException::class)
    override fun getInitialPage(): InfoItemsPage<StreamInfoItem> {
        val query = "${PeertubeParsingHelper.START_KEY}=0&" +
            "${PeertubeParsingHelper.COUNT_KEY}=${PeertubeParsingHelper.ITEMS_PER_PAGE}"
        val url = "${getUrl()}/videos?$query"
        return getPage(Page(url))
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun getPage(page: Page?): InfoItemsPage<StreamInfoItem> {
        val url = page?.url?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Page URL is required")
        val data = PeertubeParsingHelper.readApiObject(getDownloader().get(url), "playlist videos")
        PeertubeParsingHelper.validate(data)

        val items = StreamInfoItemsCollector(getServiceId())
        PeertubeParsingHelper.collectItemsFrom(items, data, getBaseUrl())
        return InfoItemsPage(items, PeertubeParsingHelper.getNextPage(url, data.getLong("total")))
    }

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val response = PeertubeParsingHelper.readApiBody(downloader.get(getUrl()), "playlist")
        val data = try {
            PeertubePlaylistPageParser.parse(response)
        } catch (error: PeertubePlaylistPageParseException) {
            throw ParsingException("Invalid PeerTube playlist response", error)
        }
        data.error?.let { throw ContentNotAvailableException(it) }
        playlist = Playlist(
            displayName = data.displayName,
            description = data.description,
            videoCount = data.videoCount,
            ownerUrl = data.ownerUrl,
            ownerName = data.ownerName,
            channelName = data.channelName,
            channelUrl = data.channelUrl,
            thumbnails = PeertubeParsingHelper.toImages(data.thumbnails, getBaseUrl()),
            ownerAvatars = PeertubeParsingHelper.toImages(data.ownerAvatars, getBaseUrl()),
            channelAvatars = PeertubeParsingHelper.toImages(data.channelAvatars, getBaseUrl()),
        )
    }

    @Throws(ParsingException::class)
    override fun getName(): String = state().displayName
        ?: throw ParsingException("Missing string field: displayName")

    private fun state(): Playlist = playlist ?: throw NullPointerException()

    private data class Playlist(
        val displayName: String?,
        val description: String?,
        val videoCount: Long,
        val ownerUrl: String?,
        val ownerName: String?,
        val channelName: String?,
        val channelUrl: String?,
        val thumbnails: List<Image>,
        val ownerAvatars: List<Image>,
        val channelAvatars: List<Image>,
    )
}
