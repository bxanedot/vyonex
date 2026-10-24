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
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLivePayloadDecoder
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLivePayloadParseException
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLiveRoom
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLiveRoomResolver
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLiveSourcePayload
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLiveStream
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLiveThumbnail
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
    private lateinit var room: MediaCCCLiveRoom

    override fun onFetchPage(downloader: Downloader) {
        val target = id
        val body = MediaCCCParsingHelper.fetchLiveStreamsBody(downloader, extractorLocalization)
        val conferences = try {
            MediaCCCLivePayloadDecoder.decode(body)
        } catch (error: MediaCCCLivePayloadParseException) {
            throw ExtractionException("Could not parse live stream data.", error)
        }
        room = MediaCCCLiveRoomResolver.findRoom(conferences, target)
            ?: throw ExtractionException("No live room matches $target")
    }

    override fun getName(): String = room.display.orEmpty()

    override fun getThumbnails(): List<Image> = Collections.unmodifiableList(
        room.thumbnails.map { thumbnail ->
            Image(
                thumbnail.url,
                Image.HEIGHT_UNKNOWN,
                Image.WIDTH_UNKNOWN,
                when (thumbnail.quality) {
                    MediaCCCLiveThumbnail.Quality.MEDIUM ->
                        Image.ResolutionLevel.MEDIUM
                    MediaCCCLiveThumbnail.Quality.HIGH ->
                        Image.ResolutionLevel.HIGH
                },
            )
        },
    )

    override fun getDescription(): Description = Description(
        "${room.conferenceDescription} - ${room.group}",
        Description.PLAIN_TEXT,
    )

    override fun getViewCount(): Long = -1

    override fun getUploaderUrl(): String =
        "https://streaming.media.ccc.de/${room.conferenceSlug}"

    override fun getUploaderName(): String = room.conferenceName.orEmpty()

    override fun getDashMpdUrl(): String = room.manifest("dash")

    override fun getHlsUrl(): String = room.manifest("hls")

    override fun getAudioStreams(): List<AudioStream> = buildList {
        for (stream in room.streams) {
            if (stream.type != "audio") continue
            for (source in stream.formats) {
                if (source.format == "dash") continue
                add(audio(source))
            }
        }
    }

    private fun audio(source: MediaCCCLiveSourcePayload): AudioStream =
        AudioStream.Builder()
            .setId(source.tech ?: Stream.ID_UNKNOWN)
            .setContent(source.url.orEmpty(), true)
            .setAverageBitrate(AudioStream.UNKNOWN_BITRATE)
            .apply {
                if (source.format == "hls") setDeliveryMethod(DeliveryMethod.HLS)
                else setMediaFormat(MediaFormat.getFromSuffix(source.format))
            }
            .build()

    override fun getVideoStreams(): List<VideoStream> = buildList {
        for (stream in room.streams) {
            if (stream.type != "video") continue
            for (source in stream.formats) {
                if (source.format == "dash") continue
                add(video(stream, source))
            }
        }
    }

    private fun video(
        stream: MediaCCCLiveStream,
        source: MediaCCCLiveSourcePayload,
    ): VideoStream =
        VideoStream.Builder()
            .setId(source.tech ?: Stream.ID_UNKNOWN)
            .setContent(source.url.orEmpty(), true)
            .setIsVideoOnly(false)
            .setResolution("${stream.width}x${stream.height}")
            .apply {
                if (source.format == "hls") setDeliveryMethod(DeliveryMethod.HLS)
                else setMediaFormat(MediaFormat.getFromSuffix(source.format))
            }
            .build()

    override fun getVideoOnlyStreams(): List<VideoStream> = emptyList()

    override fun getStreamType(): StreamType = StreamType.LIVE_STREAM

    override fun getCategory(): String = room.group

}
