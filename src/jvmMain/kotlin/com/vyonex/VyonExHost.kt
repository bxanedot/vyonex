package com.vyonex
// bxane

import com.vyonex.internal.primary.extraction.TokenProvider
import okhttp3.OkHttpClient
import java.io.File

/** Services and platform state supplied by the consuming application. */
interface VyonExHost : VyonExHostServices {
    /** Current YouTube identity. Implementations may fetch visitor data. */
    override suspend fun session(): VyonExSession

    /** Player-config storage; a disabled store keeps extraction available without persistence. */
    override val configStore: VyonExConfigStore

    /** Optional platform PoToken minting implementation. */
    val tokenProvider: TokenProvider?

    /** App-private storage for prepared player scripts, or null before host initialization. */
    val filesDir: File?

    /** Shared app transport. VyonEx configures its own Ktor client over this OkHttp client. */
    val httpClient: OkHttpClient

    override fun nowMs(): Long

    override fun log(level: VyonExLogLevel, message: String, trackId: String?)
}

// bxane
