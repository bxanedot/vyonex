// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.MediaFormat
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.PaidContentException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.linkhandler.LinkHandler
import com.vyonex.internal.fallback.localization.DateWrapper
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemsCollector
import com.vyonex.internal.fallback.stream.AudioStream
import com.vyonex.internal.fallback.stream.Description
import com.vyonex.internal.fallback.stream.StreamExtractor
import com.vyonex.internal.fallback.stream.StreamType
import com.vyonex.internal.fallback.stream.VideoStream
import com.vyonex.internal.fallback.services.bandcamp.BandcampTrackPageParseException
import com.vyonex.internal.fallback.services.bandcamp.BandcampTrackPageParser
import com.vyonex.internal.fallback.services.bandcamp.BandcampTrackPagePayload
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

public class BandcampStreamExtractor(
    service: StreamingService,
    linkHandler: LinkHandler,
) : StreamExtractor(service, linkHandler) {
    private var trackPage: TrackPage? = null

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        val html = downloader.get(linkHandler.url).responseBody()
        val payload = try {
            BandcampTrackPageParser.parse(
                BandcampExtractorHelper.getJsonAttributeText(html, "data-tralbum"),
            )
        } catch (error: BandcampTrackPageParseException) {
            throw ParsingException("Page contains invalid data-tralbum data", error)
        }
        if (payload.trackCount == 0) throw ParsingException("Bandcamp page contains no track data")
        if (payload.trackCount != 1) throw ExtractionException("The URL points to an album, not a single track")
        if (!payload.hasFile) throw PaidContentException("This track requires purchase before playback")
        trackPage = TrackPage(payload, Jsoup.parse(html))
    }

    @Throws(ParsingException::class)
    override fun getName(): String = required(data().payload.title, "title")

    @Throws(ParsingException::class)
    override fun getUploaderUrl(): String = try {
        val uri = URI(getUrl())
        if (uri.scheme == null || uri.rawAuthority == null) {
            throw URISyntaxException(getUrl(), "Track URL has no origin")
        }
        "${uri.scheme}://${uri.rawAuthority}/"
    } catch (error: URISyntaxException) {
        throw ParsingException("Bandcamp returned an invalid track URL", error)
    }

    @Throws(ParsingException::class)
    override fun getUrl(): String = https(required(data().payload.url, "url"))

    @Throws(ParsingException::class)
    override fun getUploaderName(): String = required(data().payload.artist, "artist")

    override fun getTextualUploadDate(): String? = data().payload.publishDate

    @Throws(ParsingException::class)
    override fun getUploadDate(): DateWrapper? =
        getTextualUploadDate()?.takeIf(String::isNotBlank)?.let(BandcampExtractorHelper::parseDate)

    @Throws(ParsingException::class)
    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(data().payload.artId, true)

    override fun getUploaderAvatars(): List<Image> {
        val source = data().document.selectFirst(".band-photo[src]")?.attr("src")
        return BandcampExtractorHelper.getImagesFromImageUrl(source)
    }

    override fun getDescription(): Description = Description(data().payload.description, Description.PLAIN_TEXT)

    override fun getAudioStreams(): List<AudioStream> {
        val url = data().payload.audioUrl ?: return emptyList()
        return listOf(
            AudioStream.Builder()
                .setId("mp3-128")
                .setContent(url, true)
                .setMediaFormat(MediaFormat.MP3)
                .setAverageBitrate(128)
                .build(),
        )
    }

    override fun getLength(): Long = data().payload.durationSeconds.toLong()

    override fun getVideoStreams(): List<VideoStream> = emptyList()

    override fun getVideoOnlyStreams(): List<VideoStream> = emptyList()

    override fun getStreamType(): StreamType = StreamType.AUDIO_STREAM

    override fun getRelatedItems(): PlaylistInfoItemsCollector {
        val items = PlaylistInfoItemsCollector(serviceId)
        data().document.select(".recommended-album")
            .forEach { items.commit(BandcampRelatedPlaylistInfoItemExtractor(it)) }
        return items
    }

    override fun getCategory(): String = data().document.selectFirst(".tralbum-tags .tag")?.text().orEmpty()

    override fun getLicence(): String = data().document.selectFirst("#license")?.text() ?: "Unknown"

    override fun getTags(): List<String> = data().document.select("[itemprop=keywords]")
        .map(Element::text)
        .filter(String::isNotBlank)

    private fun data(): TrackPage = checkNotNull(trackPage) { "Track page has not been fetched" }

    private fun required(value: String?, key: String): String =
        value?.takeIf(String::isNotBlank) ?: throw ParsingException("Bandcamp response is missing $key")

    private fun https(value: String): String =
        if (value.startsWith("http://")) "https://${value.substring(7)}" else value

    private data class TrackPage(
        val payload: BandcampTrackPagePayload,
        val document: Document,
    )

}
