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
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCStreamLinkHandlerFactory
import com.vyonex.internal.fallback.stream.AudioStream
import com.vyonex.internal.fallback.stream.Description
import com.vyonex.internal.fallback.stream.Stream
import com.vyonex.internal.fallback.stream.StreamExtractor
import com.vyonex.internal.fallback.stream.StreamType
import com.vyonex.internal.fallback.stream.VideoStream
import com.vyonex.internal.fallback.utils.LocaleCompat
import com.vyonex.internal.json.JsonObject

public class MediaCCCStreamExtractor(
    service: StreamingService,
    linkHandler: LinkHandler,
) : StreamExtractor(service, linkHandler) {
    private lateinit var event: Event
    private var conferenceLogo: String? = null

    override fun getTextualUploadDate(): String? = event.releaseDate

    override fun getUploadDate(): DateWrapper? = DateWrapper.fromInstant(textualUploadDate)

    override fun getThumbnails(): List<Image> = event.thumbnails

    override fun getDescription(): Description = Description(event.description, Description.PLAIN_TEXT)

    override fun getLength(): Long = event.length

    override fun getViewCount(): Long = event.viewCount

    override fun getUploaderUrl(): String = MediaCCCConferenceLinkHandlerFactory.getInstance()
        .getUrl(event.conferenceId, emptyList(), "")

    override fun getUploaderName(): String = event.conferenceId

    override fun getUploaderAvatars(): List<Image> =
        MediaCCCParsingHelper.getImageListFromLogoImageUrl(conferenceLogo)

    @Throws(ExtractionException::class)
    override fun getAudioStreams(): List<AudioStream> = buildList {
        for (recording in event.recordings) {
            if (!recording.mimeType.startsWith("audio")) continue
            val stream = AudioStream.Builder()
                .setId(recording.fileName)
                .setContent(recording.url.orEmpty(), true)
                .setMediaFormat(audioFormat(recording.mimeType))
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

    private fun audioFormat(mimeType: String): MediaFormat? = when {
        mimeType.endsWith("opus") -> MediaFormat.OPUS
        mimeType.endsWith("mpeg") -> MediaFormat.MP3
        mimeType.endsWith("ogg") -> MediaFormat.OGG
        else -> null
    }

    override fun getVideoStreams(): List<VideoStream> = buildList {
        for (recording in event.recordings) {
            if (!recording.mimeType.startsWith("video")) continue
            val format = when {
                recording.mimeType.endsWith("webm") -> MediaFormat.WEBM
                recording.mimeType.endsWith("mp4") -> MediaFormat.MPEG_4
                else -> null
            }
            add(
                VideoStream.Builder()
                    .setId(recording.fileName)
                    .setContent(recording.url.orEmpty(), true)
                    .setIsVideoOnly(false)
                    .setMediaFormat(format)
                    .setResolution("${recording.height}p")
                    .build(),
            )
        }
    }

    override fun getVideoOnlyStreams(): List<VideoStream> = emptyList()

    override fun getStreamType(): StreamType = StreamType.VIDEO_STREAM

    override fun onFetchPage(downloader: Downloader) {
        val eventUrl = MediaCCCStreamLinkHandlerFactory.VIDEO_API_ENDPOINT + id
        event = readEvent(downloader, eventUrl)
        conferenceLogo = readConferenceLogo(downloader, event.conferenceId)
    }

    private fun readEvent(downloader: Downloader, url: String): Event {
        val objectData = MediaCCCParsingHelper.fetchJsonObject(downloader, url, null)
        val conferenceReference = objectData.getString("conference_url")
        if (conferenceReference.isNullOrBlank()) {
            throw ExtractionException("The event response has no conference URL.")
        }
        val conferenceId = MediaCCCConferenceLinkHandlerFactory.getInstance()
            .getId(conferenceReference)
        val tags = objectData.getArray("tags").filterIsInstance<String>()
        val recordings = objectData.getArray("recordings")
            .streamAsJsonObjects().toList().map { recording ->
                Recording(
                    recording.getString("filename", Stream.ID_UNKNOWN).orEmpty(),
                    recording.getString("recording_url"),
                    recording.getString("mime_type", ""),
                    recording.getString("language"),
                    recording.getInt("height"),
                )
            }
        return Event(
            objectData.getString("title"),
            objectData.getString("release_date"),
            objectData.getString("description"),
            objectData.getLong("length"),
            objectData.getLong("view_count"),
            conferenceId,
            objectData.getString("frontend_link"),
            objectData.getString("original_language"),
            tags,
            recordings,
            MediaCCCParsingHelper.getThumbnailsFromStreamItem(objectData),
        )
    }

    private fun readConferenceLogo(downloader: Downloader, conferenceId: String): String? {
        val url = MediaCCCConferenceLinkHandlerFactory.CONFERENCE_API_ENDPOINT + conferenceId
        return MediaCCCParsingHelper.fetchJsonObject(downloader, url, null).getString("logo_url")
    }

    override fun getName(): String = event.title.orEmpty()

    override fun getOriginalUrl(): String = event.frontendLink.orEmpty()

    override fun getLanguageInfo(): java.util.Locale? = event.originalLanguage?.let {
        Localization.getLocaleFromThreeLetterCode(it)
    }

    override fun getTags(): List<String> = event.tags

    private data class Event(
        val title: String?,
        val releaseDate: String?,
        val description: String?,
        val length: Long,
        val viewCount: Long,
        val conferenceId: String,
        val frontendLink: String?,
        val originalLanguage: String?,
        val tags: List<String>,
        val recordings: List<Recording>,
        val thumbnails: List<Image>,
    )

    private data class Recording(
        val fileName: String,
        val url: String?,
        val mimeType: String,
        val language: String?,
        val height: Int,
    )
}
