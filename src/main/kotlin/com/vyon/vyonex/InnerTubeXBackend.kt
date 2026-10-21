package com.vyon.vyonex

import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.InnerTubeLogLevel
import com.metrolist.innertubex.InnerTubeLogger
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.PoTokenResult
import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.innertubex.extraction.TokenProviderCapabilities
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import com.metrolist.innertubex.models.YouTubeLocale
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** InnerTubeX client selection and cipher resolution, independent of Vyon app APIs. */
internal class InnerTubeXBackend(private val host: VyonExHost) {
    private var playerDir: File? = null
    private var warmup: Job? = null

    fun init() {
        playerDir = host.filesDir?.let { File(it, "innertubex_players").apply { mkdirs() } }
        scope.launch {
            cipherService.setPreprocessedPlayerCache(::readPlayer, ::writePlayer)
            warm(WARM_DELAY_MS)
        }
    }

    /** Pays the cold player-script, player-config, and PoToken costs before a track needs them. */
    private fun warm(delayMs: Long) {
        warmup?.cancel()
        warmup = scope.launch {
            delay(delayMs)
            val start = host.nowMs()
            runCatching {
                applySession()
                extractor.prewarm()
            }.onFailure { if (it is CancellationException) throw it }
                .onFailure { host.log(VyonExLogLevel.WARN, "InnerTubeX warm-up failed: ${it.message}") }
                .onSuccess { host.log(VyonExLogLevel.INFO, "InnerTubeX warmed in ${host.nowMs() - start}ms") }
        }
    }

    private fun readPlayer(key: String): String? =
        playerDir?.let { File(it, key) }?.takeIf { it.isFile }?.readText()

    private fun writePlayer(key: String, value: String?) {
        val dir = playerDir ?: return
        val file = File(dir, key)
        if (value == null) {
            file.delete()
            return
        }
        val tmp = File(dir, "$key.tmp")
        tmp.writeText(value)
        tmp.renameTo(file)
        // Players rotate every few days; only the newest are worth their megabytes.
        dir.listFiles()?.filter { !it.name.endsWith(".tmp") }?.sortedByDescending { it.lastModified() }
            ?.drop(KEPT_PLAYERS)?.forEach { it.delete() }
    }

    /** Forward the host's optional PoToken provider without changing its capabilities. */
    private val tokenProvider = object : TokenProvider {
        override val capabilities: TokenProviderCapabilities
            get() = host.tokenProvider?.capabilities
                ?: TokenProviderCapabilities(providers = emptySet(), usesWebView = false)

        override suspend fun getPoToken(videoId: String, visitorData: String, cookie: String?): PoTokenResult? =
            host.tokenProvider?.getPoToken(videoId, visitorData, cookie)

        override suspend fun close() {
            host.tokenProvider?.close()
        }
    }

    private val http = HttpClient(OkHttp) {
        engine { preconfigured = host.httpClient }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 20_000
        }
        expectSuccess = false
    }

    private val logger = InnerTubeLogger { event ->
        val details = if (event.details.isEmpty()) "" else event.details.entries.joinToString(prefix = " [", postfix = "]") {
            "${it.key}=${it.value}"
        }
        val line = "ITX ${event.tag}: ${event.message}$details"
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
        override val defaultSourceUrl: String = PLAYER_CONFIG_URL
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
    private val extractor = InnerTubeExtractor(
        configParser = YtConfigParserImpl(http, innerTube, remoteStore, logger),
        cipherService = cipherService,
        innerTube = innerTube,
        tokenProvider = tokenProvider,
        logger = logger,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val minted = ConcurrentHashMap<String, VyonExStream>()
    private val excluded = ConcurrentHashMap<String, ConcurrentHashMap<String, Long>>()

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
        if (minted.size >= MAX_REMEMBERED) minted.clear()
        minted[extracted.url] = extracted
        return extracted
    }

    fun headersFor(url: String): Map<String, String>? = minted[url]?.headers

    fun onRefused(url: String): String? {
        val refused = minted.remove(url) ?: return null
        exclude(refused.videoId, refused.profileId)
        // A refused ciphered URL may mean the remote cipher config is stale; refresh it off-path.
        scope.launch { runCatching { cipherService.refreshAfterStreamRejection() } }
        return refused.videoId
    }

    fun exclude(videoId: String, profileId: String) {
        host.log(VyonExLogLevel.WARN, "InnerTubeX: $profileId refused $videoId; skipping it for this track", videoId)
        excluded.getOrPut(videoId) { ConcurrentHashMap() }[profileId] = host.nowMs() + EXCLUDE_MS
    }

    fun onSessionChanged() {
        excluded.clear()
        minted.clear()
        if (host.configStore.enabled) warm(0)
    }

    private suspend fun applySession() {
        val session = host.session()
        innerTube.cookie = session.cookie
        innerTube.visitorData = session.visitorData
        innerTube.locale = YouTubeLocale(gl = session.region, hl = session.language)
    }

    private fun excludedFor(videoId: String): Set<String> {
        val entries = excluded[videoId] ?: return emptySet()
        val now = host.nowMs()
        entries.entries.removeAll { it.value <= now }
        return entries.keys.toSet()
    }

    private companion object {
        const val PLAYER_CONFIG_URL =
            "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/library/src/main/assets/player_configs.json"
        const val LOW_KBPS = 64
        const val EXCLUDE_MS = 10 * 60 * 1000L
        const val MAX_REMEMBERED = 64
        const val WARM_DELAY_MS = 2_000L
        const val KEPT_PLAYERS = 3
    }
}
