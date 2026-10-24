// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.MediaFormat
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.downloader.Response
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.LinkHandler
import com.vyonex.internal.fallback.localization.DateWrapper
import com.vyonex.internal.fallback.services.peertube.PeertubeCaptionResponseParser
import com.vyonex.internal.fallback.services.peertube.PeertubeMediaDelivery
import com.vyonex.internal.fallback.services.peertube.PeertubeMediaMapper
import com.vyonex.internal.fallback.services.peertube.PeertubeMediaResponseParser
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory
import com.vyonex.internal.fallback.stream.AudioStream
import com.vyonex.internal.fallback.stream.ContentAvailability
import com.vyonex.internal.fallback.stream.DeliveryMethod
import com.vyonex.internal.fallback.stream.Description
import com.vyonex.internal.fallback.stream.Frameset
import com.vyonex.internal.fallback.stream.StreamExtractor
import com.vyonex.internal.fallback.stream.StreamExtractor.Privacy
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector
import com.vyonex.internal.fallback.stream.StreamSegment
import com.vyonex.internal.fallback.stream.StreamType
import com.vyonex.internal.fallback.stream.SubtitlesStream
import com.vyonex.internal.fallback.stream.VideoStream
import com.vyonex.internal.json.JsonArray
import com.vyonex.internal.json.JsonObject
import com.vyonex.internal.json.JsonWriter
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.Locale

public class PeertubeStreamExtractor(
    service: StreamingService,
    handler: LinkHandler,
) : StreamExtractor(service, handler) {
    private lateinit var page: JsonObject
    private val optionalResponses = mutableMapOf<String, JsonObject?>()
    private val payload by lazy { PeertubeMediaResponseParser.parse(JsonWriter.string(video)) }
    private val metadata get() = payload.metadata
    private val media by lazy(::readMedia)
    private val subtitles by lazy(::readSubtitles)

    private val video: JsonObject
        get() {
            assertPageFetched()
            return page
        }

    private val apiBaseUrl: String
        get() = getBaseUrl().trimEnd('/')

    private val videoUrl: String
        get() = "$apiBaseUrl/api/v1/videos/${pathSegment(getId())}"

    @Throws(IOException::class, ExtractionException::class)
    override fun onFetchPage(downloader: Downloader) {
        page = readResponse(downloader.get(videoUrl), "video")
    }

    @Throws(ParsingException::class)
    override fun getName(): String = metadata.name
        ?: throw ParsingException("Missing string field: name")

    override fun getTextualUploadDate(): String? = metadata.publishedAt

    @Throws(ParsingException::class)
    override fun getUploadDate(): DateWrapper? = DateWrapper.fromInstant(getTextualUploadDate())

    override fun getThumbnails(): List<Image> =
        PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem(apiBaseUrl, video)

    override fun getDescription(): Description = Description(
        metadata.description, Description.MARKDOWN,
    )

    override fun getAgeLimit(): Int = if (metadata.nsfw) 18 else NO_AGE_LIMIT

    override fun getLength(): Long = metadata.duration

    override fun getViewCount(): Long = metadata.views

    override fun getLikeCount(): Long = metadata.likes

    override fun getDislikeCount(): Long = metadata.dislikes

    @Throws(ParsingException::class)
    override fun getTimeStamp(): Long = getTimestampSeconds("start=([^&#]*)").coerceAtLeast(0)

    @Throws(ParsingException::class)
    override fun getUploaderUrl(): String {
        val name = metadata.accountName
            ?: throw ParsingException("Missing string field: account.name")
        val host = metadata.accountHost
            ?: throw ParsingException("Missing string field: account.host")
        return PeertubeChannelLinkHandlerFactory.getInstance()
            .fromId("accounts/$name@$host", apiBaseUrl).url
    }

    @Throws(ParsingException::class)
    override fun getUploaderName(): String = metadata.accountDisplayName?.takeIf(String::isNotBlank)
        ?: metadata.accountName ?: throw ParsingException("Missing string field: account.name")

    override fun getUploaderAvatars(): List<Image> = PeertubeParsingHelper
        .getAvatarsFromOwnerAccountOrVideoChannelObject(apiBaseUrl, video.getObject("account"))

    override fun getSubChannelUrl(): String = metadata.channelUrl?.takeIf(String::isNotBlank) ?: ""

    override fun getSubChannelName(): String = metadata.channelDisplayName?.takeIf(String::isNotBlank)
        ?: metadata.channelName

    override fun getSubChannelAvatars(): List<Image> = PeertubeParsingHelper
        .getAvatarsFromOwnerAccountOrVideoChannelObject(apiBaseUrl, video.getObject("channel"))

    override fun getHlsUrl(): String = metadata.hlsUrl

    @Throws(IOException::class, ExtractionException::class)
    override fun getAudioStreams(): List<AudioStream> = media.audio

    @Throws(IOException::class, ExtractionException::class)
    override fun getVideoStreams(): List<VideoStream> = media.video

    @Throws(IOException::class, ExtractionException::class)
    override fun getVideoOnlyStreams(): List<VideoStream> =
        Collections.unmodifiableList(media.video.filter { it.isVideoOnly() })

    @Throws(IOException::class, ExtractionException::class)
    override fun getSubtitlesDefault(): List<SubtitlesStream> = subtitles

    @Throws(IOException::class, ExtractionException::class)
    override fun getSubtitles(format: MediaFormat?): List<SubtitlesStream> =
        if (format == null) emptyList() else subtitles.filter { it.format == format }

    override fun getStreamType(): StreamType =
        if (metadata.isLive) StreamType.LIVE_STREAM else StreamType.VIDEO_STREAM

    @Throws(IOException::class, ExtractionException::class)
    override fun getRelatedItems(): StreamInfoItemsCollector {
        val items = StreamInfoItemsCollector(serviceId)
        val tag = getTags().firstOrNull() ?: return items
        val encodedTag = URLEncoder.encode(tag, StandardCharsets.UTF_8)
        val url = "$apiBaseUrl/api/v1/search/videos?start=0&count=8&sort=-createdAt&tagsOneOf=$encodedTag"
        val response = readResponse(getDownloader().get(url), "related videos")
        val related = JsonArray()
        for (value in response.getArray("data")) {
            val item = value as? JsonObject ?: continue
            val nestedVideo = item.getObject("video").takeIf(JsonObject::isNotEmpty) ?: item
            if (nestedVideo.getString("uuid") != getId()) related.add(value)
        }
        val filtered = JsonObject(response).apply { put("data", related) }
        PeertubeParsingHelper.collectItemsFrom(items, filtered, apiBaseUrl)
        return items
    }

    override fun getHost(): String = metadata.accountHost
        ?: throw ParsingException("Missing string field: account.host")

    override fun getPrivacy(): Privacy = when (metadata.privacyId) {
        1 -> Privacy.PUBLIC
        2 -> Privacy.UNLISTED
        4 -> Privacy.PRIVATE
        5 -> Privacy.INTERNAL
        else -> Privacy.OTHER
    }

    override fun getCategory(): String = metadata.category

    override fun getLicence(): String = metadata.licence

    override fun getLanguageInfo(): Locale? = metadata.languageId
        ?.takeIf(String::isNotBlank)?.let(Locale::forLanguageTag)

    override fun getTags(): List<String> = Collections.unmodifiableList(metadata.tags)

    override fun getSupportInfo(): String = metadata.support

    @Throws(ParsingException::class)
    override fun getStreamSegments(): List<StreamSegment> = try {
        optionalResponse("/chapters", "chapters")?.getArray("chapters")
            ?.filterIsInstance<JsonObject>()
            ?.mapNotNull { chapter ->
                val seconds = chapter.getLong("timecode", -1)
                if (seconds !in 0..Int.MAX_VALUE.toLong()) return@mapNotNull null
                StreamSegment(chapter.getString("title", ""), seconds.toInt())
            } ?: emptyList()
    } catch (error: IOException) {
        throw ParsingException("Could not load PeerTube chapters", error)
    } catch (error: ExtractionException) {
        throw ParsingException("Could not load PeerTube chapters", error)
    }

    @Throws(ExtractionException::class)
    override fun getFrames(): List<Frameset> = try {
        val entries = optionalResponse("/storyboards", "storyboards")?.getArray("storyboards")
            ?: return emptyList()
        val frames = ArrayList<Frameset>(entries.size)
        for (value in entries) {
            val storyboard = value as? JsonObject ?: continue
            val width = storyboard.getInt("spriteWidth")
            val height = storyboard.getInt("spriteHeight")
            val totalWidth = storyboard.getInt("totalWidth")
            val totalHeight = storyboard.getInt("totalHeight")
            val duration = storyboard.getInt("spriteDuration")
            val image = storyboard.getString("fileUrl")
                ?.takeIf(String::isNotBlank)
                ?: storyboard.getString("storyboardPath")?.takeIf(String::isNotBlank)
            if (image == null || width <= 0 || height <= 0 || duration <= 0
                || totalWidth < width || totalHeight < height
            ) continue
            val columns = totalWidth / width
            val rows = totalHeight / height
            val count = try {
                Math.multiplyExact(columns, rows)
            } catch (_: ArithmeticException) {
                continue
            }
            frames.add(
                Frameset(listOf(resolve(image)), width, height, count, duration, columns, rows),
            )
        }
        Collections.unmodifiableList(frames)
    } catch (error: IOException) {
        throw ExtractionException("Could not load PeerTube storyboards", error)
    }

    override fun getContentAvailability(): ContentAvailability = ContentAvailability.AVAILABLE

    private fun readResponse(response: Response?, name: String): JsonObject =
        PeertubeParsingHelper.readApiObject(response, name).also(PeertubeParsingHelper::validate)

    @Throws(IOException::class, ExtractionException::class)
    private fun optionalResponse(path: String, name: String): JsonObject? {
        val url = videoUrl + path
        if (!optionalResponses.containsKey(url)) {
            val response = getDownloader().get(url)
            optionalResponses[url] = when (response.responseCode()) {
                400, 404 -> null
                else -> readResponse(response, name)
            }
        }
        return optionalResponses[url]
    }

    private fun readMedia(): MediaStreams {
        val streams = PeertubeMediaMapper.map(
            files = payload.files,
            playlists = payload.playlists,
            isLive = payload.isLive,
        )
        return MediaStreams(
            Collections.unmodifiableList(streams.audio.map { candidate ->
                val format = candidate.formatSuffix?.let(MediaFormat::getFromSuffix)
                val audioFormat = when (format) {
                    MediaFormat.WEBM -> MediaFormat.WEBMA
                    MediaFormat.MPEG_4 -> MediaFormat.M4A
                    else -> format
                }
                AudioStream.Builder()
                    .setId(candidate.id)
                    .setContent(resolve(candidate.url), true)
                    .setMediaFormat(audioFormat)
                    .setDeliveryMethod(candidate.delivery.toDeliveryMethod())
                    .setManifestUrl(candidate.manifestUrl?.let(::resolve))
                    .setAverageBitrate(candidate.bitrate)
                    .build()
            }),
            Collections.unmodifiableList(streams.video.map { candidate ->
                VideoStream.Builder()
                    .setId(candidate.id)
                    .setContent(resolve(candidate.url), true)
                    .setMediaFormat(candidate.formatSuffix?.let(MediaFormat::getFromSuffix))
                    .setDeliveryMethod(candidate.delivery.toDeliveryMethod())
                    .setManifestUrl(candidate.manifestUrl?.let(::resolve))
                    .setIsVideoOnly(candidate.isVideoOnly)
                    .setResolution(candidate.resolution)
                    .build()
            }),
        )
    }

    private fun PeertubeMediaDelivery.toDeliveryMethod(): DeliveryMethod = when (this) {
        PeertubeMediaDelivery.PROGRESSIVE_HTTP -> DeliveryMethod.PROGRESSIVE_HTTP
        PeertubeMediaDelivery.HLS -> DeliveryMethod.HLS
        PeertubeMediaDelivery.TORRENT -> DeliveryMethod.TORRENT
    }

    @Throws(IOException::class, ExtractionException::class)
    private fun readSubtitles(): List<SubtitlesStream> {
        val response = optionalResponse("/captions", "captions") ?: return emptyList()
        val captions = PeertubeCaptionResponseParser.parse(JsonWriter.string(response))
        val result = ArrayList<SubtitlesStream>(captions.size)
        for (caption in captions) {
            val format = formatOf(caption.path)
            val builder = SubtitlesStream.Builder()
                .setId(caption.language + (format?.let { "-${it.suffix}" } ?: ""))
                .setContent(resolve(caption.path), true)
                .setMediaFormat(format)
                .setDeliveryMethod(DeliveryMethod.PROGRESSIVE_HTTP)
                .setLanguageCode(caption.language)
                .setAutoGenerated(caption.autoGenerated)
            result.add(builder.build())
        }
        return Collections.unmodifiableList(result)
    }

    private fun formatOf(url: String): MediaFormat? {
        val path = url.substringBefore('?').substringBefore('#')
        val suffix = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return MediaFormat.getFromSuffix(suffix)
    }

    private fun resolve(url: String): String = try {
        val base = if (apiBaseUrl.endsWith('/')) apiBaseUrl else "$apiBaseUrl/"
        URI.create(base).resolve(url).toString()
    } catch (_: IllegalArgumentException) {
        if (url.startsWith("http://") || url.startsWith("https://")) url
        else apiBaseUrl + if (url.startsWith('/')) url else "/$url"
    }

    private fun pathSegment(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private data class MediaStreams(
        val audio: List<AudioStream>,
        val video: List<VideoStream>,
    )
}
