package com.vyon.vyonex
// bxane

/** Reusable extractor adapters, constructed once by the host application. */
class VyonEx(host: VyonExHost) {
    private val innerTubeX = InnerTubeXBackend(host)
    private val newPipe = NewPipeBackend(host)

    fun init() = innerTubeX.init()

    /** Initializes NewPipe once, before its first stream extraction. */
    fun initializeNewPipe() = newPipe.initialize()

    /** Returns playable progressive audio candidates without exposing NewPipe API types. */
    suspend fun audioCandidates(videoId: String): List<VyonExAudioCandidate> =
        newPipe.audioCandidates(videoId)

    suspend fun extract(
        videoId: String,
        maxKbps: Int,
        skipClients: Set<String> = emptySet(),
        requireM4a: Boolean = false,
    ): VyonExStream? = innerTubeX.extract(videoId, maxKbps, skipClients, requireM4a)

    fun headersFor(url: String): Map<String, String>? = innerTubeX.headersFor(url)

    fun onRefused(url: String): String? = innerTubeX.onRefused(url)

    fun exclude(videoId: String, profileId: String) = innerTubeX.exclude(videoId, profileId)

    fun onSessionChanged() = innerTubeX.onSessionChanged()
}
// bxane
