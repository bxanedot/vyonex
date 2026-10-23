// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction

import com.vyonex.internal.primary.InnerTube
import com.vyonex.internal.primary.InnerTubeLogger
import com.vyonex.internal.primary.cipher.RemotePlayerConfigStore
import com.vyonex.internal.primary.cipher.getTextWithoutRedirects
import com.vyonex.internal.primary.d
import com.vyonex.internal.primary.models.YouTubeClient
import com.vyonex.internal.primary.w
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException

public class YtConfigParserImpl(
    private val httpClient: HttpClient,
    private val innerTube: InnerTube,
    private val remotePlayerConfigStore: RemotePlayerConfigStore? = null,
    private val logger: InnerTubeLogger = InnerTubeLogger.NONE,
) : YtConfigParser {
    override suspend fun fetchConfig(videoId: String, useLoginCookies: Boolean): PlayerConfig {
        require(SAFE_VIDEO_ID.matches(videoId)) { "Invalid video ID" }
        return fetchPage(
            "https://www.youtube.com/watch?v=$videoId&bpctr=9999999999&has_verified=1",
            "watch",
            useLoginCookies,
        )
    }

    override suspend fun fetchEmbeddedConfig(videoId: String, useLoginCookies: Boolean): PlayerConfig {
        require(SAFE_VIDEO_ID.matches(videoId)) { "Invalid video ID" }
        return fetchPage(
            "https://www.youtube.com/embed/$videoId?html5=1",
            "embed",
            useLoginCookies,
            "https://www.reddit.com/",
        )
    }

    private suspend fun fetchPage(
        pageUrl: String,
        pageKind: String,
        useLoginCookies: Boolean,
        referer: String? = null,
    ): PlayerConfig {
        val cookie =
            innerTube.cookie?.takeIf { useLoginCookies }
                ?.split(';')
                ?.map(String::trim)
                ?.filter { it.isNotEmpty() && !it.startsWith("PREF=", ignoreCase = true) }
                ?.joinToString("; ")
                ?.takeIf(String::isNotEmpty)

        suspend fun fetchHtml(requestCookie: String?): String =
            getText(Url(pageUrl), PAGE_MAX_BYTES) {
                header(HttpHeaders.UserAgent, YouTubeClient.USER_AGENT_WEB)
                header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                referer?.let { header(HttpHeaders.Referrer, it) }
                requestCookie?.let { header(HttpHeaders.Cookie, it) }
                header(HttpHeaders.AcceptLanguage, innerTube.locale.acceptLanguageHeader())
                timeout {
                    requestTimeoutMillis = PAGE_TIMEOUT_MS
                    connectTimeoutMillis = PAGE_TIMEOUT_MS
                    socketTimeoutMillis = PAGE_TIMEOUT_MS
                }
            }

        val html =
            try {
                fetchHtml(cookie)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (cookie == null) throw error
                logger.w(TAG, "authenticated config page unavailable; retrying without cookies", details = errorType(error))
                fetchHtml(null)
            }

        val playerUrl = extractPlayerUrl(html) ?: fetchIframePlayerUrl()
            ?: error("Unable to parse YouTube player JavaScript URL")
        val signatureTimestamp =
            extractSignatureTimestamp(html)
                ?: remotePlayerConfigStore?.getSignatureTimestamp(playerUrl)
                ?: fetchPlayerSignatureTimestamp(playerUrl)
        val visitorData = extractVisitorData(html)
        val encryptedHostFlags = extractEncryptedHostFlags(html).takeIf { pageKind == "embed" }

        logger.d(
            TAG,
            "fetchConfig parsed page=$pageKind htmlSize=${html.length} " +
                "hasSts=${signatureTimestamp != null} hasVisitorData=${visitorData != null} " +
                "hasEncryptedHostFlags=${encryptedHostFlags != null}",
        )
        return PlayerConfig(
            playerUrl = playerUrl,
            signatureTimestamp = signatureTimestamp,
            visitorData = visitorData,
            clientVersion = extractClientVersion(html),
            encryptedHostFlags = encryptedHostFlags,
        )
    }

    private suspend fun fetchPlayerSignatureTimestamp(playerUrl: String): Int? =
        try {
            val script =
                getText(Url(playerUrl), PLAYER_JS_MAX_BYTES) {
                    header(HttpHeaders.UserAgent, YouTubeClient.USER_AGENT_WEB)
                    timeout {
                        requestTimeoutMillis = PLAYER_TIMEOUT_MS
                        connectTimeoutMillis = PLAYER_TIMEOUT_MS
                        socketTimeoutMillis = PLAYER_TIMEOUT_MS
                    }
                }
            extractSignatureTimestamp(script)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.d(TAG, "player JS timestamp unavailable type=${error::class.simpleName ?: "Exception"}")
            null
        }

    private suspend fun fetchIframePlayerUrl(): String? =
        try {
            val api =
                getText(Url(IFRAME_API_URL), IFRAME_MAX_BYTES) {
                    header(HttpHeaders.UserAgent, YouTubeClient.USER_AGENT_WEB)
                    timeout {
                        requestTimeoutMillis = PLAYER_TIMEOUT_MS
                        connectTimeoutMillis = PLAYER_TIMEOUT_MS
                        socketTimeoutMillis = PLAYER_TIMEOUT_MS
                    }
                }
            val playerId = PLAYER_ID_PATTERN.find(api.replace("\\/", "/"))?.groupValues?.get(1) ?: return null
            validatePlayerUrl("https://www.youtube.com/s/player/$playerId/player_ias.vflset/en_GB/base.js")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.d(TAG, "iframe player URL unavailable type=${error::class.simpleName ?: "Exception"}")
            null
        }

    internal fun extractPlayerUrl(html: String): String? {
        val page = html.replace("\\/", "/").replace("\\u0026", "&")
        return PLAYER_URL_PATTERNS.asSequence()
            .mapNotNull { it.find(page)?.groupValues?.get(1) }
            .mapNotNull { path ->
                val absoluteUrl = when {
                    path.startsWith("https://") -> path
                    path.startsWith('/') -> "https://www.youtube.com$path"
                    else -> "https://www.youtube.com/$path"
                }
                validatePlayerUrl(absoluteUrl)
            }
            .firstOrNull()
    }

    internal fun extractPlayerId(script: String): String? =
        PLAYER_ID_PATTERN.find(script.replace("\\/", "/"))?.groupValues?.get(1)

    internal fun extractSignatureTimestamp(text: String): Int? =
        SIGNATURE_TIMESTAMP_PATTERNS.asSequence()
            .mapNotNull { it.find(text)?.groupValues?.get(1)?.toIntOrNull() }
            .firstOrNull()

    internal fun extractClientVersion(html: String): String? = CLIENT_VERSION_PATTERN.find(html)?.groupValues?.get(1)

    internal fun extractEncryptedHostFlags(html: String): String? = ENCRYPTED_HOST_FLAGS_PATTERN.find(html)?.groupValues?.get(1)

    private fun extractVisitorData(html: String): String? = VISITOR_DATA_PATTERN.find(html)?.groupValues?.get(1)

    private suspend fun getText(
        url: Url,
        maxBytes: Int,
        configure: HttpRequestBuilder.() -> Unit,
    ): String {
        var current = url
        for (redirects in 0..MAX_REDIRECTS) {
            val response = httpClient.getTextWithoutRedirects(current, maxBytes, configure)
            if (response.status.isSuccess()) return requireNotNull(response.body)

            val location = response.headers[HttpHeaders.Location]
                ?.takeIf { response.status.value in REDIRECT_STATUS_CODES && redirects < MAX_REDIRECTS }
                ?: error("HTTP ${response.status.value}")
            current = runCatching { URLBuilder(current).takeFrom(location).build() }
                .getOrNull()
                ?.takeIf(::approvedYouTubeUrl)
                ?: error("Rejected redirect from YouTube config endpoint")
        }
        error("Too many redirects")
    }

    private fun validatePlayerUrl(value: String): String? =
        runCatching { Url(value) }.getOrNull()?.takeIf {
            it.protocol.name == "https" && it.port == 443 && approvedYouTubeHost(it.host) &&
                it.user == null && it.password == null && PLAYER_PATH.matches(it.encodedPath)
        }?.toString()

    private fun approvedYouTubeUrl(url: Url): Boolean =
        url.protocol.name == "https" && url.port == 443 && approvedYouTubeHost(url.host) &&
            url.user == null && url.password == null &&
            (url.encodedPath == "/watch" || url.encodedPath == "/iframe_api" ||
                EMBED_PATH.matches(url.encodedPath) || PLAYER_PATH.matches(url.encodedPath))

    private fun approvedYouTubeHost(host: String): Boolean =
        host == "youtube.com" || host.endsWith(".youtube.com") ||
            host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com")

    private fun errorType(error: Throwable): Map<String, String> =
        mapOf("exceptionType" to (error::class.simpleName ?: "Exception"))

    private companion object {
        const val TAG = "YtConfigParser"
        const val IFRAME_API_URL = "https://www.youtube.com/iframe_api"
        const val PAGE_TIMEOUT_MS = 10_000L
        const val PLAYER_TIMEOUT_MS = 8_000L
        const val PAGE_MAX_BYTES = 4 * 1024 * 1024
        const val PLAYER_JS_MAX_BYTES = 8 * 1024 * 1024
        const val IFRAME_MAX_BYTES = 1 * 1024 * 1024
        const val MAX_REDIRECTS = 3
        val REDIRECT_STATUS_CODES = setOf(301, 302, 303, 307, 308)
        val SAFE_VIDEO_ID = Regex("[A-Za-z0-9_-]{1,64}")
        val EMBED_PATH = Regex("/embed/[A-Za-z0-9_-]{1,64}")
        val PLAYER_PATH = Regex("/s/player/[A-Za-z0-9_-]+/.+\\.js")
        val PLAYER_URL_PATTERNS = listOf(
            Regex("\"PLAYER_JS_URL\":\"([^\"]+)\""),
            Regex("\"jsUrl\":\"([^\"]+)\""),
            Regex("(?<![A-Za-z0-9:/])(/s/player/[^\"'\\\\]+/[^\"'\\\\]*\\.js[^\"'\\\\]*)"),
        )
        val PLAYER_ID_PATTERN = Regex("/s/player/([a-zA-Z0-9_-]+)/")
        val SIGNATURE_TIMESTAMP_PATTERNS = listOf(
            Regex("(?:signatureTimestamp|sts)\"?\\s*:\\s*([0-9]{5})"),
            Regex("\"STS\":\\s*([0-9]{5})"),
        )
        val CLIENT_VERSION_PATTERN = Regex("\"INNERTUBE_CLIENT_VERSION\"\\s*:\\s*\"([^\"]+)\"")
        val ENCRYPTED_HOST_FLAGS_PATTERN = Regex("\"encryptedHostFlags\"\\s*:\\s*\"([^\"]+)\"")
        val VISITOR_DATA_PATTERN = Regex("\"visitorData\"\\s*:\\s*\"([^\"]+)\"")
    }
}
