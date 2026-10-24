// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.Page
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.PaidContentException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.playlist.PlaylistExtractor
import com.vyonex.internal.fallback.services.bandcamp.BandcampPlaylistPageParseException
import com.vyonex.internal.fallback.services.bandcamp.BandcampPlaylistPageParser
import com.vyonex.internal.fallback.services.bandcamp.BandcampPlaylistTracksPayload
import com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem.BandcampPlaylistStreamInfoItemExtractor
import com.vyonex.internal.fallback.stream.Description
import com.vyonex.internal.fallback.stream.StreamInfoItem
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

public class BandcampPlaylistExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : PlaylistExtractor(service, linkHandler) {
    private var page: AlbumPage? = null

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val html = downloader.get(linkHandler.url).responseBody()
        val document = Jsoup.parse(html)
        val tracks = try {
            BandcampPlaylistPageParser.parseTracks(
                BandcampExtractorHelper.getJsonAttributeText(html, "data-tralbum"),
            )
        } catch (error: BandcampPlaylistPageParseException) {
            throw ParsingException("Page contains invalid data-tralbum data", error)
        }
        val name = try {
            BandcampPlaylistPageParser.parseTitle(
                BandcampExtractorHelper.getJsonAttributeText(html, "data-embed"),
            )
            ?.takeIf(String::isNotBlank)
                ?: throw ParsingException("Bandcamp album response is missing its title")
        } catch (error: BandcampPlaylistPageParseException) {
            throw ParsingException("Page contains invalid data-embed data", error)
        }
        if (tracks.trackCount == 0) throw PaidContentException("This album requires purchase")

        page = AlbumPage(
            tracks = tracks,
            name = name,
            artistImages = document.selectFirst(".band-photo[src]")
                ?.attr("src")
                .let(BandcampExtractorHelper::getImagesFromImageUrl),
            description = albumDescription(document),
        )
    }

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(data().tracks.artId, true)

    @Throws(ParsingException::class)
    override fun getUploaderUrl(): String {
        val albumUrl = data().tracks.url
        try {
            val uri = URI(albumUrl ?: "")
            if (uri.host == null) throw URISyntaxException(uri.toString(), "Missing host")
            return "${uri.scheme}://${uri.rawAuthority}/"
        } catch (error: URISyntaxException) {
            throw ParsingException("Bandcamp album response has an invalid URL", error)
        }
    }

    override fun getUploaderName(): String = data().tracks.artist

    override fun getUploaderAvatars(): List<Image> = data().artistImages

    override fun isUploaderVerified(): Boolean = false

    override fun getStreamCount(): Long = data().tracks.trackCount.toLong()

    override fun getDescription(): Description = data().description

    @Throws(ExtractionException::class)
    override fun getInitialPage(): InfoItemsPage<StreamInfoItem> {
        val album = data()
        val items = StreamInfoItemsCollector(serviceId)
        var artistUrl: String? = null
        for (track in album.tracks.tracks) {
            if (artistUrl == null) artistUrl = getUploaderUrl()
            val substituteCovers = if (album.tracks.trackCount < INDIVIDUAL_COVER_LIMIT) {
                emptyList()
            } else {
                BandcampExtractorHelper.getImagesFromImageId(album.tracks.artId, true)
            }
            items.commit(
                BandcampPlaylistStreamInfoItemExtractor(
                    track.title,
                    track.titleLink,
                    track.duration,
                    track.artist,
                    artistUrl,
                    getService(),
                    substituteCovers,
                ),
            )
        }
        return InfoItemsPage(items, null)
    }

    override fun getPage(page: Page): InfoItemsPage<StreamInfoItem>? = null

    override fun getName(): String = data().name

    private fun data(): AlbumPage = checkNotNull(page) { "Bandcamp album page has not been fetched" }

    private fun albumDescription(document: Document): Description {
        val sections = listOf("#trackInfo .tralbum-about", "#trackInfo .tralbum-credits", "#license")
            .mapNotNull { selector -> document.selectFirst(selector)?.html()?.takeIf(String::isNotBlank) }
        return if (sections.isEmpty()) Description.EMPTY_DESCRIPTION
        else Description(sections.joinToString(""), Description.HTML)
    }

    private data class AlbumPage(
        val tracks: BandcampPlaylistTracksPayload,
        val name: String,
        val artistImages: List<Image>,
        val description: Description,
    )

    public companion object {
        // shortcut: use album art at ten tracks or more to avoid per-track page requests; remove when this page exposes each cover ID.
        private const val INDIVIDUAL_COVER_LIMIT = 10
    }
}
