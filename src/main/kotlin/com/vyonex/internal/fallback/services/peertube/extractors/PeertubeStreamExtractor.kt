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
    override fun getName(): String = PeertubeParsingHelper.requiredString(video, "name")

    override fun getTextualUploadDate(): String? = video.getString("publishedAt")

    @Throws(ParsingException::class)
    override fun getUploadDate(): DateWrapper? = DateWrapper.fromInstant(getTextualUploadDate())

    override fun getThumbnails(): List<Image> =
        PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem(apiBaseUrl, video)

    override fun getDescription(): Description = Description(
        video.getString("description", ""), Description.MARKDOWN,
    )

    override fun getAgeLimit(): Int = if (video.getBoolean("nsfw")) 18 else NO_AGE_LIMIT

    override fun getLength(): Long = video.getLong("duration")

    override fun getViewCount(): Long = video.getLong("views", -1)

    override fun getLikeCount(): Long = video.getLong("likes", -1)

    override fun getDislikeCount(): Long = video.getLong("dislikes", -1)

    @Throws(ParsingException::class)
    override fun getTimeStamp(): Long = getTimestampSeconds("start=([^&#]*)").coerceAtLeast(0)

    @Throws(ParsingException::class)
    override fun getUploaderUrl(): String {
        val account = video.getObject("account")
        val name = PeertubeParsingHelper.requiredString(account, "name")
        val host = PeertubeParsingHelper.requiredString(account, "host")
        return PeertubeChannelLinkHandlerFactory.getInstance()
            .fromId("accounts/$name@$host", apiBaseUrl).url
    }

    @Throws(ParsingException::class)
    override fun getUploaderName(): String {
        val account = video.getObject("account")
        return account.getString("displayName")?.takeIf(String::isNotBlank)
            ?: PeertubeParsingHelper.requiredString(account, "name")
    }

    override fun getUploaderAvatars(): List<Image> = PeertubeParsingHelper
        .getAvatarsFromOwnerAccountOrVideoChannelObject(apiBaseUrl, video.getObject("account"))

    override fun getSubChannelUrl(): String {
        val channel = video.getObject("channel")
        return channel.getString("url")?.takeIf(String::isNotBlank) ?: ""
    }

    override fun getSubChannelName(): String {
        val channel = video.getObject("channel")
        return channel.getString("displayName")?.takeIf(String::isNotBlank)
            ?: channel.getString("name", "")
    }

    override fun getSubChannelAvatars(): List<Image> = PeertubeParsingHelper
        .getAvatarsFromOwnerAccountOrVideoChannelObject(apiBaseUrl, video.getObject("channel"))

    override fun getHlsUrl(): String = video.getArray("streamingPlaylists")
        .filterIsInstance<JsonObject>()
        .firstNotNullOfOrNull { it.getString("playlistUrl")?.takeIf(String::isNotBlank) }
        ?: ""

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
        if (video.getBoolean("isLive")) StreamType.LIVE_STREAM else StreamType.VIDEO_STREAM

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

    override fun getHost(): String = PeertubeParsingHelper.requiredString(video, "account.host")

    override fun getPrivacy(): Privacy = when (video.getObject("privacy").getInt("id", -1)) {
        1 -> Privacy.PUBLIC
        2 -> Privacy.UNLISTED
        4 -> Privacy.PRIVATE
        5 -> Privacy.INTERNAL
        else -> Privacy.OTHER
    }

    override fun getCategory(): String = video.getObject("category").getString("label", "")

    override fun getLicence(): String = video.getObject("licence").getString("label", "")

    override fun getLanguageInfo(): Locale? = video.getObject("language").getString("id")
        ?.takeIf(String::isNotBlank)?.let(Locale::forLanguageTag)

    override fun getTags(): List<String> = Collections.unmodifiableList(
        video.getArray("tags").filterIsInstance<String>(),
    )

    override fun getSupportInfo(): String = video.getString("support", "")

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
        val audio = ArrayList<AudioStream>()
        val videoStreams = ArrayList<VideoStream>()

        fun addFile(file: JsonObject, method: DeliveryMethod, manifest: String? = null) {
            val resolution = file.getObject("resolution")
            val label = resolution.getString("label")?.takeIf(String::isNotBlank)
                ?: resolution.getInt("id", -1).takeIf { it > 0 }?.let { "${it}p" }
                ?: "unknown"
            for ((key, suffix) in listOf("fileUrl" to "", "fileDownloadUrl" to "-fileDownloadUrl")) {
                val rawUrl = file.getString(key)?.takeIf(String::isNotBlank) ?: continue
                if (key == "fileDownloadUrl" && rawUrl == file.getString("fileUrl")) continue
                val url = resolve(rawUrl)
                val format = formatOf(rawUrl)
                val id = if (method == DeliveryMethod.HLS) "$label-hls$suffix"
                    else "$label-${format?.suffix ?: "stream"}$suffix"
                val isAudio = resolution.getInt("id", -1) == 0
                    || label.equals("audio", ignoreCase = true)
                    || format?.mimeType?.startsWith("audio/") == true
                if (isAudio) {
                    val audioFormat = when (format) {
                        MediaFormat.WEBM -> MediaFormat.WEBMA
                        MediaFormat.MPEG_4 -> MediaFormat.M4A
                        else -> format
                    }
                    audio.add(
                        AudioStream.Builder()
                            .setId(id)
                            .setContent(url, true)
                            .setMediaFormat(audioFormat)
                            .setDeliveryMethod(method)
                            .setManifestUrl(manifest)
                            .setAverageBitrate(file.getInt("bitrate", AudioStream.UNKNOWN_BITRATE))
                            .build(),
                    )
                } else {
                    videoStreams.add(
                        VideoStream.Builder()
                            .setId(id)
                            .setContent(url, true)
                            .setMediaFormat(format)
                            .setDeliveryMethod(method)
                            .setManifestUrl(manifest)
                            .setIsVideoOnly(!file.getBoolean("hasAudio"))
                            .setResolution(label)
                            .build(),
                    )
                }
            }
        }

        video.getArray("files").filterIsInstance<JsonObject>().forEach { file ->
            addFile(file, DeliveryMethod.PROGRESSIVE_HTTP)
            file.getString("torrentUrl")?.takeIf(String::isNotBlank)?.let { torrent ->
                val label = file.getObject("resolution").getString("label", "unknown")
                videoStreams.add(
                    VideoStream.Builder()
                        .setId("$label-torrent")
                        .setContent(resolve(torrent), true)
                        .setDeliveryMethod(DeliveryMethod.TORRENT)
                        .setIsVideoOnly(false)
                        .setResolution(label)
                        .build(),
                )
            }
        }

        for (playlist in video.getArray("streamingPlaylists").filterIsInstance<JsonObject>()) {
            val manifest = playlist.getString("playlistUrl")?.takeIf(String::isNotBlank) ?: continue
            val before = audio.size + videoStreams.size
            playlist.getArray("files").filterIsInstance<JsonObject>().forEach {
                addFile(it, DeliveryMethod.HLS, resolve(manifest))
            }
            if (before == audio.size + videoStreams.size) {
                videoStreams.add(
                    VideoStream.Builder()
                        .setId(if (video.getBoolean("isLive")) "live-hls" else "hls")
                        .setContent(resolve(manifest), true)
                        .setDeliveryMethod(DeliveryMethod.HLS)
                        .setManifestUrl(resolve(manifest))
                        .setIsVideoOnly(false)
                        .setResolution("")
                        .build(),
                )
            }
        }

        return MediaStreams(
            Collections.unmodifiableList(audio),
            Collections.unmodifiableList(videoStreams),
        )
    }

    @Throws(IOException::class, ExtractionException::class)
    private fun readSubtitles(): List<SubtitlesStream> {
        val captions = optionalResponse("/captions", "captions")?.getArray("data") ?: return emptyList()
        val result = ArrayList<SubtitlesStream>(captions.size)
        for (value in captions) {
            val caption = value as? JsonObject ?: continue
            val language = caption.getObject("language").getString("id")
                ?.takeIf(String::isNotBlank) ?: continue
            val path = caption.getString("fileUrl")?.takeIf(String::isNotBlank)
                ?: caption.getString("captionPath")?.takeIf(String::isNotBlank)
                ?: caption.getString("m3u8Url")?.takeIf(String::isNotBlank)
                ?: continue
            val format = formatOf(path)
            val builder = SubtitlesStream.Builder()
                .setId(language + (format?.let { "-${it.suffix}" } ?: ""))
                .setContent(resolve(path), true)
                .setMediaFormat(format)
                .setDeliveryMethod(DeliveryMethod.PROGRESSIVE_HTTP)
                .setLanguageCode(language)
                .setAutoGenerated(caption.getBoolean("automaticallyGenerated"))
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
