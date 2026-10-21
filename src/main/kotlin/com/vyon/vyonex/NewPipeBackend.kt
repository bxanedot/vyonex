package com.vyon.vyonex
// bxane

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import java.util.concurrent.TimeUnit

/** NewPipe's watch-page fallback, isolated from Vyon app and UI types. */
internal class NewPipeBackend(private val host: VyonExHost) {
    private val init by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { NewPipe.init(OkHttpDownloader()) }

    fun initialize() { init }

    suspend fun audioCandidates(videoId: String): List<VyonExAudioCandidate> = withContext(Dispatchers.IO) {
        init
        val extractor = ServiceList.YouTube.getStreamExtractor(
            "https://www.youtube.com/watch?v=$videoId",
        )
        extractor.fetchPage()
        extractor.audioStreams
            .filter {
                !it.content.isNullOrBlank() && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP
            }
            .map { stream ->
                val format = when (stream.format) {
                    MediaFormat.M4A -> VyonExAudioFormat.M4A
                    MediaFormat.WEBMA_OPUS -> VyonExAudioFormat.WEBM_OPUS
                    else -> VyonExAudioFormat.OTHER
                }
                VyonExAudioCandidate(
                    url = stream.content,
                    kbps = stream.averageBitrate,
                    mimeType = stream.format?.mimeType.orEmpty(),
                    format = format,
                )
            }
    }

    /** NewPipe's extractor requests use a bounded, isolated transport. */
    private inner class OkHttpDownloader : Downloader() {
        override fun execute(request: Request): Response {
            if (NEXT_ENDPOINT in request.url()) {
                return Response(200, "OK", emptyMap(), EMPTY_NEXT_RESPONSE, request.url())
            }

            val builder = okhttp3.Request.Builder()
                .method(request.httpMethod(), request.dataToSend()?.toRequestBody())
                .url(request.url())
            var hasUserAgent = false
            request.headers().forEach { (name, values) ->
                if (name.equals("User-Agent", ignoreCase = true) && values.isNotEmpty()) {
                    hasUserAgent = true
                }
                when {
                    values.size > 1 -> {
                        builder.removeHeader(name)
                        values.forEach { builder.addHeader(name, it) }
                    }
                    values.size == 1 -> builder.header(name, values[0])
                }
            }
            if (!hasUserAgent) {
                builder.header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36",
                )
            }

            val startedAt = host.nowMs()
            val response = try {
                extractorClient.newCall(builder.build()).execute()
            } catch (e: Exception) {
                host.log(
                    VyonExLogLevel.WARN,
                    "extractor fetch FAILED after ${host.nowMs() - startedAt}ms " +
                        "${request.httpMethod()} ${request.url()}: ${e.javaClass.simpleName}: ${e.message}",
                )
                throw e
            }
            val elapsed = host.nowMs() - startedAt
            val level = if (elapsed > SLOW_FETCH_MS) VyonExLogLevel.WARN else VyonExLogLevel.DEBUG
            host.log(level, "extractor fetch ${elapsed}ms ${request.httpMethod()} ${request.url()}")
            if (response.code == 429) {
                response.close()
                throw ReCaptchaException("reCaptcha Challenge requested", request.url())
            }
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                response.body.string(),
                response.request.url.toString(),
            )
        }
    }

    private val extractorClient by lazy {
        host.httpClient.newBuilder()
            .connectionPool(ConnectionPool(4, 30, TimeUnit.SECONDS))
            .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
            .callTimeout(EXTRACTOR_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private companion object {
        const val NEXT_ENDPOINT = "/youtubei/v1/next"
        const val EMPTY_NEXT_RESPONSE =
            """{"responseContext":{},"contents":{},"currentVideoEndpoint":{},"trackingParams":""}"""
        const val SLOW_FETCH_MS = 2_000L
        const val PING_INTERVAL_SECONDS = 5L
        const val EXTRACTOR_TIMEOUT_SECONDS = 5L
    }
}
