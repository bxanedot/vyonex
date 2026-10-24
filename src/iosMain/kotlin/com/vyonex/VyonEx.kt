package com.vyonex

/** YouTube primary extractor exposed to Swift through the VyonEx framework. */
class VyonEx(host: VyonExHost) {
    private val primary = PrimaryExtractorBackend(host)

    fun initialize() = primary.init()

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
