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
import com.vyonex.internal.json.JsonObject
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
        val album = getAlbumInfoJson(html)
        val tracks = album.getArray("trackinfo")
        if (tracks.isEmpty()) throw ParsingException("Bandcamp page contains no track data")
        if (tracks.size != 1) throw ExtractionException("The URL points to an album, not a single track")

        val track = tracks.getObject(0)
        if (track["file"] == null) throw PaidContentException("This track requires purchase before playback")
        trackPage = TrackPage(album, album.getObject("current"), track, Jsoup.parse(html))
    }

    @Throws(ParsingException::class)
    override fun getName(): String = required(data().current, "title")

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
    override fun getUrl(): String = https(required(data().album, "url"))

    @Throws(ParsingException::class)
    override fun getUploaderName(): String = required(data().album, "artist")

    override fun getTextualUploadDate(): String? = data().current.getString("publish_date")

    @Throws(ParsingException::class)
    override fun getUploadDate(): DateWrapper? =
        getTextualUploadDate()?.takeIf(String::isNotBlank)?.let(BandcampExtractorHelper::parseDate)

    @Throws(ParsingException::class)
    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(data().album.getLong("art_id"), true)

    override fun getUploaderAvatars(): List<Image> {
        val source = data().document.selectFirst(".band-photo[src]")?.attr("src")
        return BandcampExtractorHelper.getImagesFromImageUrl(source)
    }

    override fun getDescription(): Description {
        val text = listOf("about", "lyrics", "credits")
            .mapNotNull(data().current::getString)
            .filter(String::isNotBlank)
            .joinToString("\n\n")
        return Description(text, Description.PLAIN_TEXT)
    }

    override fun getAudioStreams(): List<AudioStream> {
        val url = data().track.getObject("file").getString("mp3-128")
            ?.takeIf(String::isNotBlank) ?: return emptyList()
        return listOf(
            AudioStream.Builder()
                .setId("mp3-128")
                .setContent(url, true)
                .setMediaFormat(MediaFormat.MP3)
                .setAverageBitrate(128)
                .build(),
        )
    }

    override fun getLength(): Long = data().track.getDouble("duration").toLong()

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

    private fun required(objectValue: JsonObject, key: String): String =
        objectValue.getString(key)?.takeIf(String::isNotBlank)
            ?: throw ParsingException("Bandcamp response is missing $key")

    private fun https(value: String): String =
        if (value.startsWith("http://")) "https://${value.substring(7)}" else value

    private data class TrackPage(
        val album: JsonObject,
        val current: JsonObject,
        val track: JsonObject,
        val document: Document,
    )

    public companion object {
        @JvmStatic
        @Throws(ParsingException::class)
        public fun getAlbumInfoJson(html: String): JsonObject =
            BandcampExtractorHelper.getJsonAttribute(html, "data-tralbum")
    }
}
