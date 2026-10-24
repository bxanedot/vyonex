// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.channel.ChannelExtractor
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.linkhandler.ReadyChannelTabListLinkHandler
import com.vyonex.internal.fallback.services.bandcamp.BandcampArtistDetailsPayload
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampChannelTabLinkHandlerFactory
import org.jsoup.Jsoup
import java.io.IOException

public class BandcampChannelExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : ChannelExtractor(service, linkHandler) {
    private var artist: BandcampArtistDetailsPayload? = null
    private var downloader: Downloader? = null

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        artist = BandcampExtractorHelper.getArtistDetailsPayload(downloader, id)
        this.downloader = downloader
    }

    @Throws(ParsingException::class)
    override fun getName(): String = details().name
        ?.takeIf(String::isNotBlank)
        ?: throw ParsingException("Artist has no name")

    override fun getDescription(): String = artist?.bio ?: ""

    override fun getAvatars(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(artist?.bioImageId ?: 0, false)

    @Throws(ParsingException::class)
    override fun getBanners(): List<Image> {
        val artistUrl = artist?.url
            ?.takeIf(String::isNotBlank)
            ?: return emptyList()
        val pageDownloader = downloader ?: return emptyList()
        return try {
            val secureUrl = artistUrl.replaceFirst(Regex("^http://"), "https://")
            val html = pageDownloader.get(secureUrl)
                .responseBody()
            Jsoup.parse(html).select("#customHeader img[src]")
                .flatMap { image ->
                    BandcampExtractorHelper.getImagesFromImageUrl(image.attr("src"))
                }
        } catch (error: IOException) {
            throw ParsingException("Could not load the artist banner", error)
        } catch (error: com.vyonex.internal.fallback.exceptions.ReCaptchaException) {
            throw ParsingException("Could not load the artist banner", error)
        }
    }

    @Throws(ParsingException::class)
    override fun getTabs(): List<ListLinkHandler> {
        val discography = artist?.discography ?: return emptyList()
        val types = discography.mapNotNull { item ->
            when (item.type) {
                "track" -> ChannelTabs.TRACKS
                "album" -> ChannelTabs.ALBUMS
                else -> null
            }
        }.distinct()
        val builder = ReadyChannelTabListLinkHandler.ChannelTabExtractorBuilder { tabService, handler ->
            BandcampChannelTabExtractor.fromDiscographyPayload(tabService, handler, discography)
        }
        return types.map { type ->
            ReadyChannelTabListLinkHandler(
                url + BandcampChannelTabLinkHandlerFactory.getUrlSuffix(type),
                id,
                type,
                builder,
            )
        }
    }

    override fun getFeedUrl(): String? = null

    override fun getSubscriberCount(): Long = -1

    override fun getParentChannelName(): String? = null

    override fun getParentChannelUrl(): String? = null

    override fun getParentChannelAvatars(): List<Image> = emptyList()

    override fun isVerified(): Boolean = false

    @Throws(ParsingException::class)
    private fun details(): BandcampArtistDetailsPayload = artist
        ?: throw ParsingException("Artist details have not been fetched")
}
