// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.MediaFormat
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.linkhandler.LinkHandler
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLiveConferencePayload
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLivePayloadDecoder
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLivePayloadParseException
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLiveRoomPayload
import com.vyonex.internal.fallback.stream.AudioStream
import com.vyonex.internal.fallback.stream.DeliveryMethod
import com.vyonex.internal.fallback.stream.Description
import com.vyonex.internal.fallback.stream.Stream
import com.vyonex.internal.fallback.stream.StreamExtractor
import com.vyonex.internal.fallback.stream.StreamType
import com.vyonex.internal.fallback.stream.VideoStream
import java.util.Collections

public class MediaCCCLiveStreamExtractor(
    service: StreamingService,
    linkHandler: LinkHandler,
) : StreamExtractor(service, linkHandler) {
    private lateinit var room: LiveRoom

    override fun onFetchPage(downloader: Downloader) {
        val target = id
        val body = MediaCCCParsingHelper.fetchLiveStreamsBody(downloader, extractorLocalization)
        val conferences = try {
            MediaCCCLivePayloadDecoder.decode(body)
        } catch (error: MediaCCCLivePayloadParseException) {
            throw ExtractionException("Could not parse live stream data.", error)
        }
        for (conference in conferences) {
            val conferenceSlug = conference.slug ?: continue
            for (section in conference.groups.orEmpty()) {
                for (source in section.rooms.orEmpty()) {
                    if ("$conferenceSlug/${source.slug}" != target) continue
                    room = readRoom(conference, section.group, source)
                    return
                }
            }
        }
        throw ExtractionException("No live room matches $target")
    }

    override fun getName(): String = room.display.orEmpty()

    override fun getThumbnails(): List<Image> = room.thumbnails

    override fun getDescription(): Description = Description(
        "${room.conferenceDescription} - ${room.group}",
        Description.PLAIN_TEXT,
    )

    override fun getViewCount(): Long = -1

    override fun getUploaderUrl(): String =
        "https://streaming.media.ccc.de/${room.conferenceSlug}"

    override fun getUploaderName(): String = room.conferenceName.orEmpty()

    override fun getDashMpdUrl(): String = manifest("dash")

    override fun getHlsUrl(): String = manifest("hls")

    private fun manifest(format: String): String = room.streams.firstNotNullOfOrNull { stream ->
        stream.formats[format]?.url?.takeIf(String::isNotEmpty)
    }.orEmpty()

    override fun getAudioStreams(): List<AudioStream> = buildList {
        for (stream in room.streams) {
            if (stream.type != "audio") continue
            for ((format, source) in stream.formats) {
                if (format == "dash") continue
                add(audio(format, source))
            }
        }
    }

    private fun audio(format: String, source: MediaSource): AudioStream =
        AudioStream.Builder()
            .setId(source.id)
            .setContent(source.url.orEmpty(), true)
            .setAverageBitrate(AudioStream.UNKNOWN_BITRATE)
            .apply {
                if (format == "hls") setDeliveryMethod(DeliveryMethod.HLS)
                else setMediaFormat(MediaFormat.getFromSuffix(format))
            }
            .build()

    override fun getVideoStreams(): List<VideoStream> = buildList {
        for (stream in room.streams) {
            if (stream.type != "video") continue
            for ((format, source) in stream.formats) {
                if (format == "dash") continue
                add(video(stream, format, source))
            }
        }
    }

    private fun video(stream: LiveStream, format: String, source: MediaSource): VideoStream =
        VideoStream.Builder()
            .setId(source.id)
            .setContent(source.url.orEmpty(), true)
            .setIsVideoOnly(false)
            .setResolution("${stream.width}x${stream.height}")
            .apply {
                if (format == "hls") setDeliveryMethod(DeliveryMethod.HLS)
                else setMediaFormat(MediaFormat.getFromSuffix(format))
            }
            .build()

    override fun getVideoOnlyStreams(): List<VideoStream> = emptyList()

    override fun getStreamType(): StreamType = StreamType.LIVE_STREAM

    override fun getCategory(): String = room.group

    private data class LiveRoom(
        val display: String?,
        val conferenceName: String?,
        val conferenceSlug: String?,
        val conferenceDescription: String?,
        val group: String,
        val thumbnails: List<Image>,
        val streams: List<LiveStream>,
    )

    private data class LiveStream(
        val type: String,
        val width: Int,
        val height: Int,
        val formats: Map<String, MediaSource>,
    )

    private data class MediaSource(val id: String, val url: String?)

    private fun readRoom(
        conference: MediaCCCLiveConferencePayload,
        group: String?,
        source: MediaCCCLiveRoomPayload,
    ): LiveRoom {
        val streams = source.streams.orEmpty().map { stream ->
            val formats = linkedMapOf<String, MediaSource>()
            for (entry in stream.sources()) {
                formats[entry.format] = MediaSource(entry.tech ?: Stream.ID_UNKNOWN, entry.url)
            }
            LiveStream(
                stream.type.orEmpty(),
                stream.videoSize?.getOrNull(0) ?: 0,
                stream.videoSize?.getOrNull(1) ?: 0,
                Collections.unmodifiableMap(formats),
            )
        }
        return LiveRoom(
            source.display,
            conference.conference,
            conference.slug,
            conference.description,
            group.orEmpty(),
            thumbnails(source),
            streams,
        )
    }

    private fun thumbnails(source: MediaCCCLiveRoomPayload): List<Image> =
        Collections.unmodifiableList(buildList {
            source.thumb?.takeIf(String::isNotEmpty)?.let { url ->
                add(
                    Image(
                        url,
                        Image.HEIGHT_UNKNOWN,
                        Image.WIDTH_UNKNOWN,
                        Image.ResolutionLevel.MEDIUM,
                    ),
                )
            }
            source.poster?.takeIf(String::isNotEmpty)?.let { url ->
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
}
