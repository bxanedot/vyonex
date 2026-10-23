// bxane
package com.vyonex.internal.primary

import com.vyonex.internal.primary.models.YouTubeClient
import com.vyonex.internal.primary.models.YouTubeLocale
import com.vyonex.internal.primary.models.body.GetTranscriptBody
import com.vyonex.internal.primary.models.body.PlayerBody
import com.vyonex.internal.primary.utils.parseCookieString
import com.vyonex.internal.primary.utils.sanitizeCookieString
import com.vyonex.internal.primary.utils.sha1
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.http.userAgent
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalAtomicApi::class)
class InnerTube(
    val httpClient: HttpClient,
    private val retryDelay: suspend (kotlin.time.Duration) -> Unit = { delay(it) },
    private val logger: InnerTubeLogger = InnerTubeLogger.NONE,
) {
    private companion object {
        private const val TAG = "InnerTube"
        private const val ORIGIN_WWW = "https://www.youtube.com"
        private const val REFERER_WWW = "$ORIGIN_WWW/"
        private const val API_BASE_WWW = "$ORIGIN_WWW/youtubei/v1"

        private const val ORIGIN_MWEB = "https://m.youtube.com"
        private const val REFERER_MWEB = "$ORIGIN_MWEB/"
        private const val API_BASE_MWEB = "$ORIGIN_MWEB/youtubei/v1"

        private const val ORIGIN_MUSIC = "https://music.youtube.com"
        private const val REFERER_MUSIC = "$ORIGIN_MUSIC/"
        private const val API_BASE_MUSIC = "$ORIGIN_MUSIC/youtubei/v1"
        private const val VISITOR_DATA_URL = "$ORIGIN_WWW/sw.js_data"
        private const val TRANSCRIPT_API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"

        private const val ORIGIN_STUDIO = "https://studio.youtube.com"
        private const val REFERER_STUDIO = "$ORIGIN_STUDIO/"
        private const val API_BASE_STUDIO = "$ORIGIN_STUDIO/youtubei/v1"
        private const val TV_BEARER_PLAYER_ENDPOINT = "$API_BASE_WWW/player"
        private const val TV_BEARER_TIMEOUT_MS = 8_000L
        private const val MAX_TV_BEARER_RESPONSE_BYTES = 4 * 1024 * 1024

        private val TRANSIENT_STATUS_CODES = setOf(408, 425, 429, 500, 502, 503, 504)
        private val MEDIA_REQUEST_HEADERS =
            setOf(
                HttpHeaders.Accept.lowercase(),
                HttpHeaders.AcceptLanguage.lowercase(),
                HttpHeaders.Range.lowercase(),
                HttpHeaders.UserAgent.lowercase(),
            )
        private val VISITOR_DATA_REGEX = Regex(""""(?:VISITOR_DATA|visitorData)"\s*:\s*"([^"]+)"""")
        private const val MAX_VISITOR_RESPONSE_BYTES = 4 * 1024 * 1024
    }

    /** Immutable request identity. Contains credentials and must not be serialized or logged directly. */
    data class SessionSnapshot(
        val locale: YouTubeLocale =
            com.vyonex.internal.primary.models
                .defaultYouTubeLocale(),
        val visitorData: String? = null,
        val dataSyncId: String? = null,
        val authUser: String = "0",
        val cookie: String? = null,
        val sapisid: String? = null,
        val useLoginForBrowse: Boolean = false,
        val regionOverrideActive: Boolean = false,
        val generation: Long = 0,
    ) {
        override fun toString(): String =
            "SessionSnapshot(" +
                "locale=$locale, " +
                "visitorData=${visitorData.presence()}, " +
                "dataSyncId=${dataSyncId.presence()}, " +
                "authUser=${authUser.presence()}, " +
                "cookie=${cookie.presence()}, " +
                "sapisid=${sapisid.presence()}, " +
                "useLoginForBrowse=$useLoginForBrowse, " +
                "regionOverrideActive=$regionOverrideActive, " +
                "generation=$generation)"

        private fun String?.presence(): String = if (isNullOrBlank()) "missing" else "present"
    }

    private data class VisitorDataFlight(
        val sessionIdentity: SessionSnapshot,
        val result: CompletableDeferred<String?>,
    )

    private data class SessionBoundRequest(
        val session: SessionSnapshot,
        val job: Job,
    )

    private val session = AtomicReference(SessionSnapshot())
    private val mutableSessionFlow = MutableStateFlow(session.load())
    val sessionFlow: StateFlow<SessionSnapshot> = mutableSessionFlow.asStateFlow()

    private val visitorDataFetchMutex = Mutex()
    private val visitorDataScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var visitorDataFlight: VisitorDataFlight? = null
    private val sessionBoundRequests = AtomicReference<List<SessionBoundRequest>>(emptyList())

    var locale: YouTubeLocale
        get() = session.load().locale
        set(value) = updateSession { it.copy(locale = value) }

    var visitorData: String?
        get() = session.load().visitorData
        set(value) = updateSession { it.copy(visitorData = value) }

    var dataSyncId: String?
        get() = session.load().dataSyncId
        set(value) = updateSession { it.copy(dataSyncId = value) }

    var authUser: String
        get() = session.load().authUser
        set(value) = updateSession { it.copy(authUser = sanitizeAuthUser(value)) }

    var cookie: String?
        get() = session.load().cookie
        set(value) {
            val sanitized = value?.let(::sanitizeCookieString)
            updateSession { it.copy(cookie = sanitized, sapisid = resolveSapisidValue(sanitized)) }
        }

    var useLoginForBrowse: Boolean
        get() = session.load().useLoginForBrowse
        set(value) = updateSession { it.copy(useLoginForBrowse = value) }

    var regionOverrideActive: Boolean
        get() = session.load().regionOverrideActive
        set(value) = updateSession { it.copy(regionOverrideActive = value) }

    fun createIsolatedSession(includeAccount: Boolean): InnerTube {
        val current = sessionSnapshot()
        return InnerTube(httpClient, retryDelay, logger).also { isolated ->
            isolated.locale = current.locale
            isolated.regionOverrideActive = current.regionOverrideActive
            isolated.replaceSession(
                cookie = current.cookie.takeIf { includeAccount },
                visitorData = current.visitorData,
                dataSyncId = current.dataSyncId.takeIf { includeAccount },
                authUser = current.authUser.takeIf { includeAccount } ?: "0",
                useLoginForBrowse = current.useLoginForBrowse && includeAccount,
            )
        }
    }

    /**
     * Cancels library-owned visitor-data work and session-bound requests.
     * The caller retains ownership of [httpClient].
     */
    fun close() {
        visitorDataScope.cancel()
        while (true) {
            val requests = sessionBoundRequests.load()
            if (sessionBoundRequests.compareAndSet(requests, emptyList())) {
                requests.forEach { it.job.cancel() }
                return
            }
        }
    }

    private inline fun updateSession(transform: (SessionSnapshot) -> SessionSnapshot) {
        while (true) {
            val current = session.load()
            val updated = transform(current)
            if (updated == current) return
            if (publishSession(current, updated)) return
        }
    }

    fun sessionSnapshot(): SessionSnapshot = session.load()

    fun sessionSnapshotWithVisitorData(
        expected: SessionSnapshot,
        visitorData: String,
    ): SessionSnapshot? {
        val current = session.load()
        return current.takeIf {
            expected.copy(visitorData = visitorData, generation = current.generation) == current
        }
    }

    fun clearSessionIfMatches(expected: SessionSnapshot): Boolean =
        publishSession(
            expected,
            expected.copy(
                visitorData = null,
                dataSyncId = null,
                authUser = "0",
                cookie = null,
                sapisid = null,
                useLoginForBrowse = false,
            ),
        )

    private fun publishSession(
        expected: SessionSnapshot,
        updated: SessionSnapshot,
    ): Boolean {
        if (updated == expected) return session.load() == expected
        val versioned = updated.copy(generation = expected.generation + 1)
        if (!session.compareAndSet(expected, versioned)) return false
        while (true) {
            val published = mutableSessionFlow.value
            if (published.generation >= versioned.generation) break
            if (mutableSessionFlow.compareAndSet(published, versioned)) break
        }
        cancelStaleSessionRequests(versioned.generation)
        return true
    }

    private fun cancelStaleSessionRequests(currentGeneration: Long) {
        while (true) {
            val requests = sessionBoundRequests.load()
            val active = requests.filter { it.session.generation >= currentGeneration }
            if (!sessionBoundRequests.compareAndSet(requests, active)) continue
            requests
                .asSequence()
                .filter { it.session.generation < currentGeneration }
                .forEach { it.job.cancel(CancellationException("InnerTube session changed")) }
            return
        }
    }

    private suspend fun <T> withSessionBoundRequest(
        requestSession: SessionSnapshot,
        block: suspend () -> T,
    ): T {
        val job = currentCoroutineContext()[Job] ?: return block()
        val request = SessionBoundRequest(requestSession, job)
        while (true) {
            if (session.load().generation != requestSession.generation) throw CancellationException("InnerTube session changed")
            val requests = sessionBoundRequests.load()
            if (sessionBoundRequests.compareAndSet(requests, requests + request)) break
        }
        try {
            if (session.load().generation != requestSession.generation) throw CancellationException("InnerTube session changed")
            return block()
        } finally {
            while (true) {
                val requests = sessionBoundRequests.load()
                if (request !in requests || sessionBoundRequests.compareAndSet(requests, requests - request)) break
            }
        }
    }

    fun replaceSession(
        cookie: String?,
        visitorData: String?,
        dataSyncId: String?,
        authUser: String,
        useLoginForBrowse: Boolean,
    ) {
        val sanitizedCookie = cookie?.let(::sanitizeCookieString)
        updateSession { current ->
            current.copy(
                visitorData = visitorData,
                dataSyncId = dataSyncId,
                authUser = sanitizeAuthUser(authUser),
                cookie = sanitizedCookie,
                sapisid = resolveSapisidValue(sanitizedCookie),
                useLoginForBrowse = useLoginForBrowse,
            )
        }
    }

    private fun sanitizeAuthUser(value: String): String = value.filter { it.isDigit() }.ifBlank { "0" }

    private fun resolveSapisidValue(cookie: String?): String? {
        val cookieMap = cookie?.let(::parseCookieString).orEmpty()
        return cookieMap["SAPISID"]
            ?: cookieMap["__Secure-3PAPISID"]
            ?: cookieMap["__Secure-1PAPISID"]
    }

    private fun sapisidAuthHeader(
        origin: String,
        sapisid: String?,
    ): String? {
        sapisid ?: return null
        val currentTime = Clock.System.now().toEpochMilliseconds() / 1000
        val sapisidHash = sha1("$currentTime $sapisid $origin")
        return "SAPISIDHASH ${currentTime}_$sapisidHash"
    }

    /** SAPISID present so SAPISIDHASH auth can be sent for [YouTubeClient.loginSupported] clients. */
    fun hasSapCookieAuth(): Boolean = hasSapCookieAuth(session.load())

    fun hasSapCookieAuth(session: SessionSnapshot): Boolean = !session.cookie.isNullOrBlank() && !session.sapisid.isNullOrBlank()

    private fun HttpRequestBuilder.ytClient(
        client: YouTubeClient,
        session: SessionSnapshot,
        setLogin: Boolean = false,
        visitorData: String? = session.visitorData,
        includeAccountCookies: Boolean = true,
    ) {
        val endpoint = url.encodedPath.removePrefix("/")
        val (apiBase, origin, referer) =
            when {
                endpoint == "player" && client.useMusicPlayerEndpoint -> {
                    Triple(API_BASE_MUSIC, ORIGIN_MUSIC, REFERER_MUSIC)
                }

                client.clientName == "WEB_REMIX" -> {
                    Triple(API_BASE_MUSIC, ORIGIN_MUSIC, REFERER_MUSIC)
                }

                client.clientName == "WEB_CREATOR" -> {
                    Triple(API_BASE_STUDIO, ORIGIN_STUDIO, REFERER_STUDIO)
                }

                client.clientName == "MWEB" -> {
                    Triple(API_BASE_MWEB, ORIGIN_MWEB, REFERER_MWEB)
                }

                else -> {
                    Triple(API_BASE_WWW, ORIGIN_WWW, REFERER_WWW)
                }
            }
        val requestReferer = if (client.isEmbedded) "https://www.reddit.com/" else referer

        contentType(ContentType.Application.Json)
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("Origin", origin)
            append("X-Origin", origin)
            append("Referer", requestReferer)
            append(
                "Accept-Language",
                session.locale.acceptLanguageHeader(),
            )
            visitorData?.let { append("X-Goog-Visitor-Id", it) }
            if (client.loginSupported && includeAccountCookies) {
                val effectiveCookie = injectPrefCookie(session.cookie, session.locale.hl, session.locale.gl)
                append(HttpHeaders.Cookie, effectiveCookie)
            }
            if (setLogin && client.loginSupported && !session.cookie.isNullOrBlank()) {
                append("X-Goog-AuthUser", session.authUser)
                val sapisid = session.sapisid
                if (!sapisid.isNullOrBlank()) {
                    val currentTime = Clock.System.now().toEpochMilliseconds() / 1000
                    val sapisidHash = sha1("$currentTime $sapisid $origin")
                    append("Authorization", "SAPISIDHASH ${currentTime}_$sapisidHash")
                }
            }
        }

        url("$apiBase/$endpoint")
        parameter("prettyPrint", false)
        userAgent(client.userAgent)
    }

    private suspend fun executeRequest(
        operation: String = "request",
        retryTransientFailures: Boolean = true,
        requireSuccess: Boolean = true,
        maxAttempts: Int = 3,
        initialDelay: Long = 500L,
        factor: Double = 2.0,
        block: suspend () -> HttpResponse,
    ): HttpResponse {
        var currentDelay = initialDelay
        var attempt = 0
        while (true) {
            try {
                val response = block()
                val retryableStatus = response.status.value in TRANSIENT_STATUS_CODES
                if (!retryTransientFailures || !retryableStatus || attempt + 1 >= maxAttempts) {
                    if (attempt > 0 && !retryableStatus) {
                        logger.w(TAG, "$operation succeeded on retry ${attempt + 1}/$maxAttempts")
                    }
                    if (requireSuccess && response.status.value !in 200..299) {
                        response.bodyAsChannel().cancel(null)
                        throw InnerTubeHttpException(operation, response.status)
                    }
                    return response
                }
                response.bodyAsChannel().cancel(null)
                attempt++
                logger.w(
                    TAG,
                    "$operation returned HTTP ${response.status.value} on attempt $attempt/$maxAttempts, retrying in ${currentDelay}ms",
                )
                retryDelay(currentDelay.milliseconds)
                currentDelay = (currentDelay * factor).toLong()
            } catch (e: CancellationException) {
                throw e
            } catch (e: InnerTubeHttpException) {
                throw e
            } catch (e: Exception) {
                attempt++
                if (!retryTransientFailures || attempt >= maxAttempts) {
                    logger.w(TAG, "$operation failed after $attempt attempts (${e.logType()})")
                    throw e
                }
                logger.w(
                    TAG,
                    "$operation failed on attempt $attempt/$maxAttempts (${e.logType()}), retrying in ${currentDelay}ms",
                )
                retryDelay(currentDelay.milliseconds)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
    }

    suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String? = null,
        signatureTimestamp: Int? = null,
        poToken: String? = null,
        requestVisitorData: String? = null,
        encryptedHostFlags: String? = null,
    ): HttpResponse =
        playerWithSession(
            client,
            videoId,
            playlistId,
            signatureTimestamp,
            poToken,
            requestVisitorData,
            sessionSnapshot(),
            encryptedHostFlags,
        )

    internal suspend fun playerWithSession(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
        signatureTimestamp: Int?,
        poToken: String?,
        requestVisitorData: String?,
        requestSession: SessionSnapshot,
        encryptedHostFlags: String? = null,
    ): HttpResponse {
        val visitorDataForRequest = requestVisitorData ?: requestSession.visitorData
        val requestClient =
            if ("{language}" in client.userAgent) {
                client.copy(userAgent = client.userAgent.replace("{language}", requestSession.locale.hl))
            } else {
                client
            }
        return executeRequest(operation = "player:${client.clientName}") {
            val startTime = Clock.System.now().toEpochMilliseconds()
            val hasCookie = !requestSession.cookie.isNullOrBlank()
            val hasSapisid = !requestSession.sapisid.isNullOrBlank()
            val authHeaderEligible = client.loginSupported
            val hasDataSync = !requestSession.dataSyncId.isNullOrBlank()
            logger.d(
                TAG,
                "player request client=${client.clientName} version=${client.clientVersion} " +
                    "poToken=${poToken != null} sts=${signatureTimestamp != null} " +
                    "loginSupported=${client.loginSupported} loginRequired=${client.loginRequired} " +
                    "hasCookie=$hasCookie hasSapisid=$hasSapisid authEligible=$authHeaderEligible hasDataSync=$hasDataSync",
            )
            val response =
                httpClient.post("player") {
                    ytClient(requestClient, requestSession, setLogin = true, visitorData = visitorDataForRequest)
                    setBody(
                        PlayerBody(
                            context =
                                requestClient.toContext(
                                    requestSession.locale,
                                    visitorDataForRequest,
                                    requestSession.dataSyncId.takeIf { client.loginSupported },
                                ),
                            videoId = videoId,
                            playlistId = playlistId,
                            playbackContext =
                                if (client.useSignatureTimestamp || client.isEmbedded) {
                                    PlayerBody.PlaybackContext(
                                        PlayerBody.PlaybackContext.ContentPlaybackContext(
                                            html5Preference =
                                                "HTML5_PREF_WANTS".takeUnless { client.useMusicPlayerEndpoint },
                                            signatureTimestamp = signatureTimestamp.takeIf { client.useSignatureTimestamp },
                                            encryptedHostFlags = encryptedHostFlags.takeIf { client.isEmbedded },
                                        ),
                                    )
                                } else {
                                    null
                                },
                            thirdParty =
                                if (client.isEmbedded) {
                                    PlayerBody.ThirdParty(embedUrl = "https://www.youtube.com/embed/$videoId")
                                } else {
                                    null
                                },
                            serviceIntegrityDimensions =
                                if (poToken != null) {
                                    PlayerBody.ServiceIntegrityDimensions(poToken)
                                } else {
                                    null
                                },
                            contentCheckOk = true,
                            racyCheckOk = true,
                            videoCheckOk = true.takeUnless { client.useMusicPlayerEndpoint },
                        ),
                    )
                    userAgent(requestClient.userAgent)
                }
            val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
            logger.d(TAG, "player response client=${client.clientName} elapsed=${elapsed}ms")
            response
        }
    }

    suspend fun mediaContentLength(
        url: String,
        requestHeaders: Map<String, String>,
    ): Long? =
        try {
            val mediaUrl = validatedMediaUrl(url)
            httpClient
                .head(mediaUrl) {
                    requestHeaders.forEach { (name, value) ->
                        if (name.lowercase() in MEDIA_REQUEST_HEADERS) header(name, value)
                    }
                }.contentLength()
                ?.takeIf { it > 0L }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    suspend fun playerWithSessionBound(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
        signatureTimestamp: Int?,
        poToken: String?,
        requestVisitorData: String?,
        requestSession: SessionSnapshot,
        encryptedHostFlags: String? = null,
    ): HttpResponse =
        withSessionBoundRequest(requestSession) {
            playerWithSession(
                client,
                videoId,
                playlistId,
                signatureTimestamp,
                poToken,
                requestVisitorData,
                requestSession,
                encryptedHostFlags,
            )
        }

    /**
     * Sends one bearer-only TV player request. The returned body is bounded and the request never follows
     * redirects or retries, so the bearer cannot be propagated to another endpoint.
     */
    internal suspend fun playerWithTvBearerSessionBound(
        client: YouTubeClient,
        videoId: String,
        signatureTimestamp: Int?,
        requestSession: SessionSnapshot,
        bearerToken: String,
    ): String? =
        withSessionBoundRequest(requestSession) {
            require(bearerToken.length in 1..4096 && bearerToken.all { it.code in 0x21..0x7e }) {
                "TV bearer credential is not header-safe"
            }
            val requestClient =
                if ("{language}" in client.userAgent) {
                    client.copy(userAgent = client.userAgent.replace("{language}", requestSession.locale.hl))
                } else {
                    client
                }
            val body =
                PlayerBody(
                    context = requestClient.toContext(requestSession.locale, requestSession.visitorData, null),
                    videoId = videoId,
                    playlistId = null,
                    playbackContext =
                        if (requestClient.useSignatureTimestamp || requestClient.isEmbedded) {
                            PlayerBody.PlaybackContext(
                                PlayerBody.PlaybackContext.ContentPlaybackContext(
                                    html5Preference = "HTML5_PREF_WANTS".takeUnless { requestClient.useMusicPlayerEndpoint },
                                    signatureTimestamp = signatureTimestamp.takeIf { requestClient.useSignatureTimestamp },
                                ),
                            )
                        } else {
                            null
                        },
                    thirdParty = null,
                    serviceIntegrityDimensions = null,
                    contentCheckOk = true,
                    racyCheckOk = true,
                    videoCheckOk = true.takeUnless { requestClient.useMusicPlayerEndpoint },
                )
            val directClient =
                HttpClient(httpClient.engine) {
                    expectSuccess = false
                    followRedirects = false
                    install(HttpTimeout)
                }
            try {
                val response =
                    directClient.post(TV_BEARER_PLAYER_ENDPOINT) {
                        contentType(ContentType.Application.Json)
                        headers {
                            append("X-Goog-Api-Format-Version", "1")
                            append("X-YouTube-Client-Name", requestClient.clientId)
                            append("X-YouTube-Client-Version", requestClient.clientVersion)
                            append("Origin", ORIGIN_WWW)
                            append("X-Origin", ORIGIN_WWW)
                            append("Referer", REFERER_WWW)
                            append("Accept-Language", requestSession.locale.acceptLanguageHeader())
                            requestSession.visitorData?.takeIf(String::isNotBlank)?.let { append("X-Goog-Visitor-Id", it) }
                            append(HttpHeaders.Authorization, "Bearer $bearerToken")
                        }
                        parameter("prettyPrint", false)
                        setBody(Json.encodeToString(PlayerBody.serializer(), body))
                        userAgent(requestClient.userAgent)
                        timeout {
                            requestTimeoutMillis = TV_BEARER_TIMEOUT_MS
                            connectTimeoutMillis = TV_BEARER_TIMEOUT_MS
                            socketTimeoutMillis = TV_BEARER_TIMEOUT_MS
                        }
                    }
                if (!response.status.isSuccess()) {
                    response.bodyAsChannel().cancel(null)
                    null
                } else {
                    response.bodyAsTextLimited(MAX_TV_BEARER_RESPONSE_BYTES)
                }
            } finally {
                directClient.close()
            }
        }

    private fun validatedMediaUrl(value: String): Url {
        val url = Url(value)
        require(
            url.protocol.name == "https" &&
                url.port == 443 &&
                (url.host == "googlevideo.com" || url.host.endsWith(".googlevideo.com")) &&
                url.encodedPath == "/videoplayback" &&
                url.user == null &&
                url.password == null,
        ) {
            "Media URL must use an approved Google Video HTTPS endpoint"
        }
        return url
    }

    suspend fun getTranscript(
        client: YouTubeClient,
        videoId: String,
    ): HttpResponse {
        val requestSession = sessionSnapshot()
        return executeRequest(operation = "getTranscript") {
            httpClient.post("https://music.youtube.com/youtubei/v1/get_transcript") {
                // YouTube Music's public web-client key, not an application credential.
                parameter("key", TRANSCRIPT_API_KEY)
                headers {
                    append("Content-Type", "application/json")
                }
                setBody(
                    GetTranscriptBody(
                        context = client.toContext(requestSession.locale, null, null),
                        params =
                            Base64.encode(
                                "\n${11.toChar()}$videoId".encodeToByteArray(),
                            ),
                    ),
                )
            }
        }
    }

    suspend fun fetchFreshVisitorData(): String? = fetchFreshVisitorData(sessionSnapshot(), forceRefresh = true)

    suspend fun fetchFreshVisitorData(
        requestSession: SessionSnapshot,
        forceRefresh: Boolean = false,
    ): String? {
        val deferred =
            visitorDataFetchMutex.withLock {
                val currentSession = session.load()
                if (!forceRefresh || requestSession != currentSession) {
                    currentSession.visitorData
                        ?.takeIf { visitorData ->
                            visitorData.isNotBlank() && requestSession.copy(visitorData = visitorData) == currentSession
                        }?.let { visitorData ->
                            return@withLock CompletableDeferred<String?>().also { it.complete(visitorData) }
                        }
                }

                val sessionIdentity = requestSession.copy(visitorData = null)
                visitorDataFlight
                    ?.takeIf { it.sessionIdentity == sessionIdentity }
                    ?.result
                    ?: CompletableDeferred<String?>().also { result ->
                        visitorDataFlight = VisitorDataFlight(sessionIdentity, result)
                        visitorDataScope.launch {
                            try {
                                result.complete(fetchFreshVisitorDataNetwork(requestSession))
                            } catch (e: Throwable) {
                                result.completeExceptionally(e)
                            } finally {
                                withContext(NonCancellable) {
                                    visitorDataFetchMutex.withLock {
                                        if (visitorDataFlight?.result === result) visitorDataFlight = null
                                    }
                                }
                            }
                        }
                    }
            }
        return deferred.await()
    }

    private suspend fun fetchFreshVisitorDataNetwork(requestSession: SessionSnapshot): String? =
        try {
            val startTime = Clock.System.now().toEpochMilliseconds()
            logger.d(TAG, "fetchFreshVisitorData start")
            val serviceWorkerData =
                try {
                    val response =
                        executeRequest(operation = "fetchFreshVisitorData", requireSuccess = false) {
                            httpClient.get(VISITOR_DATA_URL) {
                                header("User-Agent", YouTubeClient.USER_AGENT_WEB)
                                header(HttpHeaders.Accept, "application/json,text/plain,*/*")
                                header(
                                    HttpHeaders.AcceptLanguage,
                                    requestSession.locale.acceptLanguageHeader(),
                                )
                            }
                        }
                    if (response.status.value in 200..299) {
                        parseServiceWorkerVisitorData(response.bodyAsTextLimited(MAX_VISITOR_RESPONSE_BYTES))
                    } else {
                        response.bodyAsChannel().cancel(null)
                        null
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    logger.d(TAG, "service-worker visitor data unavailable (${error.logType()})")
                    null
                }
            val data =
                serviceWorkerData
                    ?: fetchHomepageVisitorData(requestSession)
            if (!data.isNullOrBlank()) {
                publishSession(requestSession, requestSession.copy(visitorData = data))
            }
            val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
            logger.d(
                TAG,
                "fetchFreshVisitorData success hasVisitorData=${!data.isNullOrBlank()} source=${if (serviceWorkerData != null) "service-worker" else "homepage"} elapsed=${elapsed}ms",
            )
            data
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.w(TAG, "fetchFreshVisitorData failed (${e.logType()})")
            null
        }

    private suspend fun fetchHomepageVisitorData(requestSession: SessionSnapshot): String? {
        val response =
            executeRequest(operation = "fetchFreshVisitorData", requireSuccess = false) {
                httpClient.get(ORIGIN_MUSIC) {
                    header("User-Agent", YouTubeClient.USER_AGENT_WEB)
                    header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    header(
                        HttpHeaders.AcceptLanguage,
                        requestSession.locale.acceptLanguageHeader(),
                    )
                }
            }
        if (response.status.value !in 200..299) {
            response.bodyAsChannel().cancel(null)
            return null
        }
        return VISITOR_DATA_REGEX
            .find(response.bodyAsTextLimited(MAX_VISITOR_RESPONSE_BYTES))
            ?.groupValues
            ?.get(1)
    }

    private fun parseServiceWorkerVisitorData(responseText: String): String? =
        runCatching {
            Json
                .parseToJsonElement(responseText.substringAfter('\n').trimStart())
                .jsonArray[0]
                .jsonArray[2]
                .jsonArray[0]
                .jsonArray[0]
                .jsonArray[13]
                .jsonPrimitive
                .contentOrNull
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
}

class InnerTubeHttpException(
    val operation: String,
    val status: HttpStatusCode,
) : IllegalStateException("$operation failed with HTTP ${status.value}")

private fun Throwable.logType(): String = this::class.simpleName ?: "Exception"

private fun injectPrefCookie(
    cookie: String?,
    hl: String,
    gl: String,
): String {
    if (cookie.isNullOrBlank()) return "PREF=hl=$hl&gl=$gl"
    val parts = cookie.split(";").map { it.trim() }.filter { it.isNotEmpty() }
    val prefParts = mutableListOf<String>()
    val otherParts = mutableListOf<String>()
    var foundPref = false
    for (part in parts) {
        if (part.startsWith("PREF=", ignoreCase = true)) {
            foundPref = true
            val prefValue = part.substringAfter("=")
            prefValue.split("&").filterTo(prefParts) { !it.startsWith("hl=") && !it.startsWith("gl=") }
        } else {
            otherParts.add(part)
        }
    }
    prefParts.add(0, "hl=$hl")
    prefParts.add(1, "gl=$gl")
    val newPref = "PREF=${prefParts.joinToString("&")}"
    return (otherParts + newPref).joinToString("; ")
}
