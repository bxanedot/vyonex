// bxane
package com.vyonex

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers

internal actual fun platformExtractorDependencies(host: VyonExHostServices): PlatformExtractorDependencies {
    val iosHost = host as? VyonExHost ?: error("iOS extraction requires VyonExHost")
    return PlatformExtractorDependencies(
        httpClient = HttpClient(Darwin) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 20_000
            }
            expectSuccess = false
        },
        backgroundDispatcher = Dispatchers.Default,
        tokenProvider = iosHost.tokenProvider,
        // shortcut: iOS player scripts stay memory-only; add host storage callbacks when persistence is needed.
        readPlayer = { null },
        writePlayer = { _, _ -> },
    )
}
