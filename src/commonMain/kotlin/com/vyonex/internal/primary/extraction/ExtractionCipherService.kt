// bxane
package com.vyonex.internal.primary.extraction

import com.vyonex.internal.primary.models.response.PlayerResponse.StreamingData.Format

internal interface ExtractionCipherService {
    suspend fun initialize()

    suspend fun preloadPlayerCode(playerUrl: String)

    suspend fun prewarmPlayerCipher()

    suspend fun processFormats(
        playerUrl: String,
        formats: List<Format>,
    ): List<Format>
}
