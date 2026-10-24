// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.MediaFormat
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ContentNotSupportedException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.exceptions.ReCaptchaException
import com.vyonex.internal.fallback.linkhandler.LinkHandler
import com.vyonex.internal.fallback.localization.DateWrapper
import com.vyonex.internal.fallback.stream.AudioStream
import com.vyonex.internal.fallback.stream.Description
import com.vyonex.internal.fallback.stream.StreamExtractor
import com.vyonex.internal.fallback.stream.StreamSegment
import com.vyonex.internal.fallback.stream.StreamType
import com.vyonex.internal.fallback.stream.VideoStream
import com.vyonex.internal.fallback.services.bandcamp.BandcampRadioEpisodeParseException
import com.vyonex.internal.fallback.services.bandcamp.BandcampRadioEpisodeParser
import com.vyonex.internal.fallback.services.bandcamp.BandcampRadioEpisodePayload
import org.jsoup.Jsoup
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

public class BandcampRadioStreamExtractor(
    service: StreamingService,
    linkHandler: LinkHandler,
) : StreamExtractor(service, linkHandler) {
    private var show: BandcampRadioEpisodePayload? = null

    @Throws(ParsingException::class)
    override fun onFetchPage(downloader: Downloader) {
        val showId = getId().toIntOrNull()
            ?: throw ParsingException("Invalid Bandcamp radio show ID")
        show = fetchShow(downloader, showId)
    }

    @Throws(ParsingException::class)
    override fun getName(): String = required(data().subtitle, "subtitle")

    @Throws(ContentNotSupportedException::class)
    override fun getUploaderUrl(): String =
        throw ContentNotSupportedException("Bandcamp radio has no uploader page")

    override fun getUrl(): String = linkHandler.url

    @Throws(ParsingException::class)
    override fun getUploaderName(): String {
        val caption = data().imageCaption.orEmpty()
        return Jsoup.parse(caption).selectFirst("a")?.text()?.takeIf(String::isNotBlank)
            ?: throw ParsingException("Bandcamp radio response has no host name")
    }

    override fun getTextualUploadDate(): String? = data().publishedDate

    @Throws(ParsingException::class)
    override fun getUploadDate(): DateWrapper? {
        val date = data().publishedDate?.takeIf(String::isNotBlank) ?: return null
        return try {
            DateWrapper(LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant())
        } catch (error: DateTimeParseException) {
            throw ParsingException("Invalid Bandcamp radio publication date", error)
        }
    }

    override fun getThumbnails(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(data().imageId, false)

    override fun getUploaderAvatars(): List<Image> = listOf(
        Image(HOST_AVATAR, 512, 512, Image.ResolutionLevel.MEDIUM),
    )

    override fun getDescription(): Description = Description(data().description, Description.PLAIN_TEXT)

    override fun getLength(): Long = data().duration

    override fun getAudioStreams(): List<AudioStream> = buildList(2) {
        addAudio(data().mp3Url, "mp3-128", MediaFormat.MP3, 128)?.let(::add)
        addAudio(data().opusUrl, "opus-lo", MediaFormat.OPUS, 100)?.let(::add)
    }

    override fun getStreamSegments(): List<StreamSegment> = data().segments.map { segment ->
        StreamSegment(segment.title, segment.timecode).apply {
            previewUrl = BandcampExtractorHelper.getImageUrl(segment.artId, true)
            channelName = segment.artist
        }
    }

    override fun getVideoStreams(): List<VideoStream> = emptyList()

    override fun getVideoOnlyStreams(): List<VideoStream> = emptyList()

    override fun getStreamType(): StreamType = StreamType.AUDIO_STREAM

    private fun data(): BandcampRadioEpisodePayload = checkNotNull(show) {
        "Bandcamp radio page has not been fetched"
    }

    private fun required(value: String?, field: String): String = value?.takeIf(String::isNotBlank)
        ?: throw ParsingException("Bandcamp radio response is missing $field")

    private fun addAudio(url: String?, id: String, format: MediaFormat, bitrate: Int): AudioStream? {
        if (url.isNullOrBlank()) return null
        return AudioStream.Builder()
            .setId(id)
            .setContent(url, true)
            .setMediaFormat(format)
            .setAverageBitrate(bitrate)
            .build()
    }

    private fun fetchShow(downloader: Downloader, id: Int): BandcampRadioEpisodePayload {
        val response = try {
            downloader.get("$API_URL$id")
        } catch (error: IOException) {
            throw ParsingException("Could not load Bandcamp radio show", error)
        } catch (error: ReCaptchaException) {
            throw ParsingException("Could not load Bandcamp radio show", error)
        }
        val body = response?.responseBody()
        if (body.isNullOrBlank() || response.responseCode() >= 400) {
            throw ParsingException("Bandcamp returned no radio-show data")
        }
        return try {
            BandcampRadioEpisodeParser.parse(body)
        } catch (error: BandcampRadioEpisodeParseException) {
            throw ParsingException("Could not load Bandcamp radio show", error)
        }
    }

    public companion object {
        private const val API_URL = "${BandcampExtractorHelper.BASE_API_URL}/bcweekly/1/get?id="
        private const val HOST_AVATAR = "${BandcampExtractorHelper.BASE_URL}/img/buttons/bandcamp-button-circle-whitecolor-512.png"
    }
}
