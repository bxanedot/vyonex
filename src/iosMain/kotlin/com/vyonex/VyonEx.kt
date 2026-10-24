package com.vyonex
// bxane

/** YouTube primary and watch-page fallback extraction exposed through the VyonEx framework. */
class VyonEx(host: VyonExHost) {
    private val primary = PrimaryExtractorBackend(host)

    fun initialize() = primary.init()

    /** Retained for API parity; fallback extraction needs no separate initialization. */
    fun initializeFallback() = Unit

    suspend fun audioCandidates(videoId: String): List<VyonExAudioCandidate> = primary.audioCandidates(videoId)

    suspend fun extract(
        videoId: String,
        maxKbps: Int,
        skipClients: Set<String> = emptySet(),
        requireM4a: Boolean = false,
    ): VyonExStream? = primary.extractWithWatchPageFallback(videoId, maxKbps, skipClients, requireM4a)

    fun headersFor(url: String): Map<String, String>? = primary.headersFor(url)

    fun onRefused(url: String): String? = primary.onRefused(url)

    fun exclude(videoId: String, profileId: String) = primary.exclude(videoId, profileId)

    fun onSessionChanged() = primary.onSessionChanged()
}
