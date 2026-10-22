package com.vyonex
// bxane

import com.vyonex.internal.primary.extraction.TokenProvider
import okhttp3.OkHttpClient
import java.io.File

/** Services and platform state supplied by the consuming application. */
interface VyonExHost {
    /** Current YouTube identity. Implementations may fetch visitor data. */
    suspend fun session(): VyonExSession

    /** Player-config storage; a disabled store keeps extraction available without persistence. */
    val configStore: VyonExConfigStore

    /** Optional platform PoToken minting implementation. */
    val tokenProvider: TokenProvider?

    /** App-private storage for preprocessed player scripts, or null before host initialization. */
    val filesDir: File?

    /** Shared app transport. VyonEx configures its own Ktor client over this OkHttp client. */
    val httpClient: OkHttpClient

    fun nowMs(): Long

    fun log(level: VyonExLogLevel, message: String, trackId: String? = null)
}

interface VyonExConfigStore {
    val enabled: Boolean

    fun getString(key: String): String?
    fun getLong(key: String): Long?
    fun putString(key: String, value: String)
    fun putLong(key: String, value: Long)
}

data class VyonExSession(
    val cookie: String?,
    val visitorData: String?,
    val region: String,
    val language: String,
)

enum class VyonExLogLevel { DEBUG, INFO, WARN, ERROR }
// bxane
