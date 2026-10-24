// bxane
package com.vyonex

import com.vyonex.internal.primary.extraction.TokenProvider
import io.ktor.client.HttpClient

internal data class PlatformExtractorDependencies(
    val httpClient: HttpClient,
    val tokenProvider: TokenProvider?,
    val readPlayer: suspend (String) -> String?,
    val writePlayer: suspend (String, String?) -> Unit,
)

internal expect fun platformExtractorDependencies(host: VyonExHostServices): PlatformExtractorDependencies
