// bxane
package com.vyonex.internal.primary.extraction

import com.vyonex.internal.primary.cipher.YouTubeCipherService
import com.vyonex.internal.primary.models.response.PlayerResponse.StreamingData.Format

internal class DefaultExtractionCipherService(
    private val delegate: YouTubeCipherService,
) : ExtractionCipherService {
    override suspend fun initialize() = delegate.initialize()

    override suspend fun preloadPlayerCode(playerUrl: String) = delegate.preloadPlayerCode(playerUrl)

    override suspend fun prewarmPlayerCipher() = delegate.prewarmPlayerCipher()

    override suspend fun processFormats(
        playerUrl: String,
        formats: List<Format>,
    ): List<Format> = delegate.processFormats(playerUrl, formats)
}
