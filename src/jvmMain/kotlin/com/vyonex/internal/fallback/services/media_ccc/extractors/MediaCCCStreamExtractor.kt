// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.MediaFormat
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.LinkHandler
import com.vyonex.internal.fallback.localization.DateWrapper
import com.vyonex.internal.fallback.localization.Localization
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCEventParseException
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCEventPayload
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCListingPayloadDecoder
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCListingParseException
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCRecordingMapper
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCStreamLinkHandlerFactory
import com.vyonex.internal.fallback.stream.AudioStream
import com.vyonex.internal.fallback.stream.Description
import com.vyonex.internal.fallback.stream.Stream
import com.vyonex.internal.fallback.stream.StreamExtractor
import com.vyonex.internal.fallback.stream.StreamType
import com.vyonex.internal.fallback.stream.VideoStream
import com.vyonex.internal.fallback.utils.LocaleCompat
import java.util.Collections

public class MediaCCCStreamExtractor(
    service: StreamingService,
    linkHandler: LinkHandler,
) : StreamExtractor(service, linkHandler) {
    private lateinit var event: MediaCCCEventPayload
    private val recordings get() = MediaCCCRecordingMapper.map(event.recordings)
    private lateinit var conferenceId: String
    private var conferenceLogo: String? = null

    override fun getTextualUploadDate(): String? = event.releaseDate

    override fun getUploadDate(): DateWrapper? = DateWrapper.fromInstant(textualUploadDate)

    override fun getThumbnails(): List<Image> = eventThumbnails(event)

    override fun getDescription(): Description = Description(event.description, Description.PLAIN_TEXT)

    override fun getLength(): Long = event.length ?: 0L

    override fun getViewCount(): Long = event.viewCount ?: 0L

    override fun getUploaderUrl(): String = MediaCCCConferenceLinkHandlerFactory.getInstance()
        .getUrl(conferenceId, emptyList(), "")

    override fun getUploaderName(): String = conferenceId

    override fun getUploaderAvatars(): List<Image> =
        MediaCCCParsingHelper.getImageListFromLogoImageUrl(conferenceLogo)

    @Throws(ExtractionException::class)
    override fun getAudioStreams(): List<AudioStream> = buildList {
        for (recording in recordings.audio) {
            val stream = AudioStream.Builder()
                .setId(recording.filename ?: Stream.ID_UNKNOWN)
                .setContent(recording.url.orEmpty(), true)
                .setMediaFormat(recording.formatSuffix?.let(MediaFormat::getFromSuffix))
                .setAverageBitrate(AudioStream.UNKNOWN_BITRATE)
            val language = recording.language
            if (language != null && '-' !in language) {
                val locale = LocaleCompat.forLanguageTag(language).orElse(null)
                    ?: throw ParsingException("Cannot convert language to a locale: $language")
                stream.setAudioLocale(locale)
            }
            add(stream.build())
        }
    }

    override fun getVideoStreams(): List<VideoStream> = buildList {
        for (recording in recordings.video) {
            add(
                VideoStream.Builder()
                    .setId(recording.filename ?: Stream.ID_UNKNOWN)
                    .setContent(recording.url.orEmpty(), true)
                    .setIsVideoOnly(false)
                    .setMediaFormat(recording.formatSuffix?.let(MediaFormat::getFromSuffix))
                    .setResolution("${recording.height ?: 0}p")
                    .build(),
            )
        }
    }

    override fun getVideoOnlyStreams(): List<VideoStream> = emptyList()

    override fun getStreamType(): StreamType = StreamType.VIDEO_STREAM

    override fun onFetchPage(downloader: Downloader) {
        val eventUrl = MediaCCCStreamLinkHandlerFactory.VIDEO_API_ENDPOINT + id
        event = readEvent(downloader, eventUrl)
        conferenceLogo = readConferenceLogo(downloader, conferenceId)
    }

    private fun readEvent(downloader: Downloader, url: String): MediaCCCEventPayload {
        val body = MediaCCCParsingHelper.fetchJsonBody(downloader, url, null)
        val payload = try {
            MediaCCCEventPayload.decode(body)
        } catch (error: MediaCCCEventParseException) {
            throw ExtractionException("Could not parse event API response from $url", error)
        }
        val conferenceReference = payload.conferenceUrl
        if (conferenceReference.isNullOrBlank()) {
            throw ExtractionException("The event response has no conference URL.")
        }
        conferenceId = MediaCCCConferenceLinkHandlerFactory.getInstance()
            .getId(conferenceReference)
        return payload
    }

    private fun eventThumbnails(payload: MediaCCCEventPayload): List<Image> =
        Collections.unmodifiableList(buildList {
            payload.thumbUrl?.takeIf(String::isNotEmpty)?.let { url ->
                add(
                    Image(
                        url,
                        Image.HEIGHT_UNKNOWN,
                        Image.WIDTH_UNKNOWN,
                        Image.ResolutionLevel.MEDIUM,
                    ),
                )
            }
            payload.posterUrl?.takeIf(String::isNotEmpty)?.let { url ->
                add(
                    Image(
                        url,
                        Image.HEIGHT_UNKNOWN,
                        Image.WIDTH_UNKNOWN,
                        Image.ResolutionLevel.HIGH,
                    ),
                )
            }
        })

    private fun readConferenceLogo(downloader: Downloader, conferenceId: String): String? {
        val url = MediaCCCConferenceLinkHandlerFactory.CONFERENCE_API_ENDPOINT + conferenceId
        return try {
            MediaCCCListingPayloadDecoder.decodeConference(
                MediaCCCParsingHelper.fetchJsonBody(downloader, url, null),
            ).logoUrl
        } catch (error: MediaCCCListingParseException) {
            throw ExtractionException("Could not parse API response from $url", error)
        }
    }

    override fun getName(): String = event.title.orEmpty()

    override fun getOriginalUrl(): String = event.frontendLink.orEmpty()

    override fun getLanguageInfo(): java.util.Locale? = event.originalLanguage?.let {
        Localization.getLocaleFromThreeLetterCode(it)
    }

    override fun getTags(): List<String> = event.stringTags()
}
