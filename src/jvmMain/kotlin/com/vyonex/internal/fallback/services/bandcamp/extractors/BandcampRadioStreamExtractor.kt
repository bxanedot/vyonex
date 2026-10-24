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
import com.vyonex.internal.json.JsonObject
import com.vyonex.internal.json.JsonParser
import com.vyonex.internal.json.JsonParserException
import org.jsoup.Jsoup
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

public class BandcampRadioStreamExtractor(
    service: StreamingService,
    linkHandler: LinkHandler,
) : StreamExtractor(service, linkHandler) {
    private var show: RadioShow? = null

    @Throws(ParsingException::class)
    override fun onFetchPage(downloader: Downloader) {
        val showId = getId().toIntOrNull()
            ?: throw ParsingException("Invalid Bandcamp radio show ID")
        show = parseShow(fetchShow(downloader, showId))
    }

    @Throws(ParsingException::class)
    override fun getName(): String = required(data().title, "subtitle")

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

    private fun data(): RadioShow = checkNotNull(show) {
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

    private fun parseShow(data: JsonObject): RadioShow {
        val segments = data.getArray("tracks").mapNotNull { value ->
            val track = value as? JsonObject ?: return@mapNotNull null
            val title = track.getString("title")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            RadioSegment(
                title = title,
                timecode = track.getInt("timecode"),
                artId = track.getLong("track_art_id"),
                artist = track.getString("artist"),
            )
        }
        val audio = data.getObject("audio_stream")
        return RadioShow(
            title = data.getString("subtitle"),
            imageCaption = data.getString("image_caption"),
            publishedDate = data.getString("published_date"),
            imageId = data.getLong("show_image_id"),
            description = data.getString("desc"),
            duration = data.getLong("audio_duration"),
            mp3Url = audio.getString("mp3-128"),
            opusUrl = audio.getString("opus-lo"),
            segments = segments,
        )
    }

    private fun fetchShow(downloader: Downloader, id: Int): JsonObject {
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
            JsonParser.`object`().from(body)
        } catch (error: JsonParserException) {
            throw ParsingException("Could not load Bandcamp radio show", error)
        }
    }

    private data class RadioShow(
        val title: String?,
        val imageCaption: String?,
        val publishedDate: String?,
        val imageId: Long,
        val description: String?,
        val duration: Long,
        val mp3Url: String?,
        val opusUrl: String?,
        val segments: List<RadioSegment>,
    )

    private data class RadioSegment(
        val title: String,
        val timecode: Int,
        val artId: Long,
        val artist: String?,
    )

    public companion object {
        private const val API_URL = "${BandcampExtractorHelper.BASE_API_URL}/bcweekly/1/get?id="
        private const val HOST_AVATAR = "${BandcampExtractorHelper.BASE_URL}/img/buttons/bandcamp-button-circle-whitecolor-512.png"
    }
}
