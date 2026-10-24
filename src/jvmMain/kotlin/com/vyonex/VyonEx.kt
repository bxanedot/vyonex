package com.vyonex
// bxane

/** Reusable extractor adapters, constructed once by the host application. */
class VyonEx(host: VyonExHost) {
    private val primary = PrimaryExtractorBackend(host)

    fun init() = primary.init()

    /** Retained for source compatibility; fallback extraction needs no separate initialization. */
    fun initializeFallback() = Unit

    /** Returns playable progressive audio candidates without exposing implementation types. */
    suspend fun audioCandidates(videoId: String): List<VyonExAudioCandidate> =
        primary.audioCandidates(videoId)

    suspend fun extract(
        videoId: String,
        maxKbps: Int,
        skipClients: Set<String> = emptySet(),
        requireM4a: Boolean = false,
    ): VyonExStream? = primary.extract(videoId, maxKbps, skipClients, requireM4a)

    fun headersFor(url: String): Map<String, String>? = primary.headersFor(url)

    fun onRefused(url: String): String? = primary.onRefused(url)

    fun exclude(videoId: String, profileId: String) = primary.exclude(videoId, profileId)

    fun onSessionChanged() = primary.onSessionChanged()
}
// bxane
