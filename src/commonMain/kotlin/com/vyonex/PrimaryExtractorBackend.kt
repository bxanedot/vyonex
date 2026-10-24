package com.vyonex
// bxane

import com.vyonex.internal.primary.InnerTube
import com.vyonex.internal.primary.InnerTubeLogLevel
import com.vyonex.internal.primary.InnerTubeLogger
import com.vyonex.internal.primary.cipher.PlayerConfigRepository
import com.vyonex.internal.primary.cipher.RemotePlayerConfigStore
import com.vyonex.internal.primary.cipher.DEFAULT_PLAYER_CONFIG_URL
import com.vyonex.internal.primary.cipher.YouTubeCipherService
import com.vyonex.internal.primary.extraction.AudioQuality
import com.vyonex.internal.primary.extraction.ContentHints
import com.vyonex.internal.primary.extraction.DefaultExtractionCipherService
import com.vyonex.internal.primary.extraction.InnerTubeExtractor
import com.vyonex.internal.primary.extraction.PlayerClientDirector
import com.vyonex.internal.primary.extraction.strategy.ContentAwareFallbackStrategy
import com.vyonex.internal.primary.extraction.PoTokenResult
import com.vyonex.internal.primary.extraction.TokenProvider
import com.vyonex.internal.primary.extraction.TokenProviderCapabilities
import com.vyonex.internal.primary.extraction.YtConfigParserImpl
import com.vyonex.internal.primary.extraction.isAllowedMediaUrl
import com.vyonex.internal.primary.extraction.selectBestAudioFormat
import com.vyonex.internal.primary.extraction.generateClientPlaybackNonce
import com.vyonex.internal.primary.models.YouTubeClient
import com.vyonex.internal.primary.models.YouTubeLocale
import com.vyonex.internal.primary.models.response.PlayerResponse.StreamingData.Format
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Primary client selection and cipher resolution. */
internal class PrimaryExtractorBackend(
    private val host: VyonExHostServices,
    private val platform: PlatformExtractorDependencies = platformExtractorDependencies(host),
) {
    private val stateLock = PlatformStateLock()
    private var warmup: Job? = null
    private val minted = mutableMapOf<String, VyonExStream>()
    private val excluded = mutableMapOf<String, MutableMap<String, Long>>()

    fun init() {
        scope.launch {
            cipherService.setPreprocessedPlayerCache(platform.readPlayer, platform.writePlayer)
            warm(WARM_DELAY_MS)
        }
    }

    /** Pays the cold player-script, player-config, and PoToken costs before a track needs them. */
    private fun warm(delayMs: Long) {
        stateLock.withLock {
            warmup?.cancel()
            warmup = scope.launch {
                delay(delayMs)
                val start = host.nowMs()
                runCatching {
                    applySession()
                    extractor.prewarm()
                }.onFailure { if (it is CancellationException) throw it }
                    .onFailure { host.log(VyonExLogLevel.WARN, "VyonEx warm-up failed: ${it.message}") }
                    .onSuccess { host.log(VyonExLogLevel.INFO, "VyonEx warmed in ${host.nowMs() - start}ms") }
            }
        }
    }

    /** Forward the host's optional PoToken provider without changing its capabilities. */
    private val tokenProvider = object : TokenProvider {
        override val capabilities: TokenProviderCapabilities
            get() = platform.tokenProvider?.capabilities
                ?: TokenProviderCapabilities(providers = emptySet(), usesWebView = false)

        override suspend fun getPoToken(videoId: String, visitorData: String, cookie: String?): PoTokenResult? =
            platform.tokenProvider?.getPoToken(videoId, visitorData, cookie)

        override suspend fun close() {
            platform.tokenProvider?.close()
        }
    }

    private val http = platform.httpClient

    private val logger = InnerTubeLogger { event ->
        val details = if (event.details.isEmpty()) "" else event.details.entries.joinToString(prefix = " [", postfix = "]") {
            "${it.key}=${it.value}"
        }
        val line = "VyonEx ${event.tag}: ${event.message}$details"
        val level = when (event.level) {
            InnerTubeLogLevel.DEBUG -> VyonExLogLevel.DEBUG
            InnerTubeLogLevel.INFO -> VyonExLogLevel.INFO
            else -> VyonExLogLevel.WARN
        }
        host.log(level, line)
    }

    private val repository = object : PlayerConfigRepository {
        override val enabled: Boolean get() = host.configStore.enabled
        override val sourceUrl: String = PLAYER_CONFIG_URL
        override var cachedJson: String
            get() = host.configStore.getString("json").orEmpty()
            set(value) { host.configStore.putString("json", value) }
        override var cachedAtMs: Long
            get() = host.configStore.getLong("cached_at_ms") ?: 0L
            set(value) { host.configStore.putLong("cached_at_ms", value) }
        override var cachedSourceUrl: String
            get() = host.configStore.getString("source_url").orEmpty()
            set(value) { host.configStore.putString("source_url", value) }
        override var cachedEtag: String
            get() = host.configStore.getString("etag").orEmpty()
            set(value) { host.configStore.putString("etag", value) }
    }

    private val innerTube = InnerTube(http, logger = logger)
    private val remoteStore = RemotePlayerConfigStore(http, repository, logger)
    private val cipherService = YouTubeCipherService(http, remoteStore, logger)
    private val configParser = YtConfigParserImpl(http, innerTube, remoteStore, logger)
    private val extractor = InnerTubeExtractor(
        configParser = configParser,
        clientDirector = PlayerClientDirector(
            innerTube = innerTube,
            fallbackStrategy = ContentAwareFallbackStrategy(),
            tokenProvider = tokenProvider,
            logger = logger,
        ),
        cipherService = DefaultExtractionCipherService(cipherService),
        innerTube = innerTube,
        tokenProvider = tokenProvider,
        logger = logger,
    )

    private val scope = CoroutineScope(SupervisorJob() + platform.backgroundDispatcher)

    suspend fun extract(
        videoId: String,
        maxKbps: Int,
        skipClients: Set<String> = emptySet(),
        requireM4a: Boolean = false,
    ): VyonExStream? {
        applySession()
        val stream = extractor.extract(
            videoId = videoId,
            hints = ContentHints().withStreamCapabilities(allowHls = false, allowSabr = false, allowBoundedRange = true),
            excludedClients = excludedFor(videoId) + skipClients,
            audioQuality = when {
                requireM4a -> AudioQuality.MP4
                maxKbps <= LOW_KBPS -> AudioQuality.LOW
                else -> AudioQuality.AUTO
            },
            clientPlaybackNonce = generateClientPlaybackNonce(),
        ) ?: return null
        check(stream.sabrBootstrap == null) { "SABR is not supported by this playback engine" }
        val mime = stream.mimeType.orEmpty()
        val extracted = VyonExStream(
            videoId = videoId,
            url = stream.audioUrl,
            kbps = (stream.bitrate ?: 0) / 1000,
            mimeType = if (stream.codecs.isNullOrBlank()) mime else "$mime; codecs=\"${stream.codecs}\"",
            loudnessDb = stream.loudnessDb,
            clientName = stream.clientName,
            profileId = stream.profileId,
            headers = stream.headers,
        )
        stateLock.withLock {
            if (minted.size >= MAX_REMEMBERED) minted.clear()
            minted[extracted.url] = extracted
        }
        return extracted
    }

    suspend fun extractWithWatchPageFallback(
        videoId: String,
        maxKbps: Int,
        skipClients: Set<String> = emptySet(),
        requireM4a: Boolean = false,
    ): VyonExStream? {
        val primaryStream =
            try {
                extract(videoId, maxKbps, skipClients, requireM4a)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                host.log(
                    VyonExLogLevel.WARN,
                    "VyonEx primary extraction failed; trying watch-page fallback: ${error::class.simpleName}",
                    videoId,
                )
                null
            }
        return primaryStream ?: extractWatchPageFallback(videoId, maxKbps, skipClients, requireM4a)
    }

    internal suspend fun extractWatchPageFallback(
        videoId: String,
        maxKbps: Int,
        skipClients: Set<String> = emptySet(),
        requireM4a: Boolean = false,
    ): VyonExStream? {
        val excluded = excludedFor(videoId)
        if (YouTubeClient.WEB.clientName in skipClients || WATCH_PAGE_PROFILE in skipClients ||
            YouTubeClient.WEB.clientName in excluded || WATCH_PAGE_PROFILE in excluded
        ) return null

        applySession()
        val includeLoginCookies = innerTube.hasSapCookieAuth()
        val page = configParser.fetchWatchPage(videoId, useLoginCookies = includeLoginCookies) ?: return null
        val response = page.playerResponse
        if (response.playabilityStatus.status != "OK") return null

        val formats =
            response.streamingData?.let { it.formats.orEmpty() + it.adaptiveFormats }
                ?.filter(Format::isAudio)
                .orEmpty()
        val resolvedFormats =
            page.playerUrl?.let { cipherService.processFormats(it, formats) } ?: formats
        val audioQuality =
            when {
                requireM4a -> AudioQuality.MP4
                maxKbps <= LOW_KBPS -> AudioQuality.LOW
                else -> AudioQuality.AUTO
            }
        val format =
            selectBestAudioFormat(
                resolvedFormats.filter { it.url?.let(::isAllowedMediaUrl) == true },
                audioQuality,
            ) ?: return null
        val url = format.url ?: return null
        val headers =
            linkedMapOf(
                "User-Agent" to YouTubeClient.USER_AGENT_WEB,
                "Accept" to "*/*",
                "Accept-Language" to innerTube.locale.acceptLanguageHeader(),
                "Referer" to "https://www.youtube.com/",
                "Origin" to "https://www.youtube.com",
            )
        if (includeLoginCookies) {
            innerTube.sessionSnapshot().cookie?.takeIf(String::isNotBlank)?.let { headers["Cookie"] = it }
        }

        val stream =
            VyonExStream(
                videoId = videoId,
                url = url,
                kbps = format.bitrate / 1_000,
                mimeType = format.mimeType,
                loudnessDb = response.playerConfig?.audioConfig?.loudnessDb ?: format.loudnessDb,
                clientName = "WEB",
                profileId = WATCH_PAGE_PROFILE,
                headers = headers,
            )
        stateLock.withLock {
            if (minted.size >= MAX_REMEMBERED) minted.clear()
            minted[stream.url] = stream
        }
        host.log(VyonExLogLevel.INFO, "VyonEx watch-page fallback selected audio itag=${format.itag}", videoId)
        return stream
    }

    fun headersFor(url: String): Map<String, String>? = stateLock.withLock { minted[url]?.headers }

    fun onRefused(url: String): String? {
        val refused = stateLock.withLock { minted.remove(url) } ?: return null
        exclude(refused.videoId, refused.profileId)
        // A refused ciphered URL may mean the remote cipher config is stale; refresh it off-path.
        scope.launch { runCatching { cipherService.refreshAfterStreamRejection() } }
        return refused.videoId
    }

    fun exclude(videoId: String, profileId: String) {
        host.log(VyonExLogLevel.WARN, "VyonEx: $profileId refused $videoId; skipping it for this track", videoId)
        stateLock.withLock {
            excluded.getOrPut(videoId) { mutableMapOf() }[profileId] = host.nowMs() + EXCLUDE_MS
        }
    }

    fun onSessionChanged() {
        stateLock.withLock {
            excluded.clear()
            minted.clear()
        }
        if (host.configStore.enabled) warm(0)
    }

    private suspend fun applySession() {
        val session = host.session()
        innerTube.cookie = session.cookie
        innerTube.visitorData = session.visitorData
        innerTube.locale = YouTubeLocale(gl = session.region, hl = session.language)
    }

    private fun excludedFor(videoId: String): Set<String> {
        return stateLock.withLock {
            val entries = excluded[videoId] ?: return@withLock emptySet()
            val now = host.nowMs()
            entries.entries.removeAll { it.value <= now }
            entries.keys.toSet()
        }
    }

    private companion object {
        const val PLAYER_CONFIG_URL = DEFAULT_PLAYER_CONFIG_URL
        const val LOW_KBPS = 64
        const val EXCLUDE_MS = 10 * 60 * 1000L
        const val MAX_REMEMBERED = 64
        const val WARM_DELAY_MS = 2_000L
        const val WATCH_PAGE_PROFILE = "WEB_WATCH_PAGE"
    }
}
// bxane
