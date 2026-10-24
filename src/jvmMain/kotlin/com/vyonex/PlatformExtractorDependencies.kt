// bxane
package com.vyonex

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import kotlinx.serialization.json.Json

internal actual fun platformExtractorDependencies(host: VyonExHostServices): PlatformExtractorDependencies {
    val jvmHost = host as? VyonExHost ?: error("JVM extraction requires VyonExHost")
    val playerDir = jvmHost.filesDir?.let { File(it, "vyonex_players").apply { mkdirs() } }
    val http = HttpClient(OkHttp) {
        engine { preconfigured = jvmHost.httpClient }
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
    return PlatformExtractorDependencies(
        httpClient = http,
        tokenProvider = jvmHost.tokenProvider,
        readPlayer = { key -> playerDir?.let { File(it, key) }?.takeIf { it.isFile }?.readText() },
        writePlayer = { key, value ->
            val dir = playerDir
            if (dir != null) {
                val file = File(dir, key)
                if (value == null) {
                    file.delete()
                } else {
                    val tmp = File(dir, "$key.tmp")
                    tmp.writeText(value)
                    tmp.renameTo(file)
                    dir.listFiles()?.filter { !it.name.endsWith(".tmp") }?.sortedByDescending { it.lastModified() }
                        ?.drop(3)?.forEach { it.delete() }
                }
            }
        },
    )
}
