package com.vyonex
// bxane

import com.vyonex.internal.primary.extraction.TokenProvider
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.jupiter.api.io.TempDir

class PlatformExtractorDependenciesTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun playerCacheUsesFilesDirectoryWhenHostInitializesItLater() = runTest {
        val host = TestHost()
        val dependencies = platformExtractorDependencies(host)

        assertNull(dependencies.readPlayer("player.js"))
        host.filesDir = tempDir.toFile()
        dependencies.writePlayer("player.js", "player-script")

        assertEquals("player-script", dependencies.readPlayer("player.js"))
    }

    @Test
    fun legacyFallbackInitializationRemainsAvailableWithoutANewPipeBackend() {
        VyonEx(TestHost()).initializeFallback()
    }

    private class TestHost : VyonExHost {
        override suspend fun session() = VyonExSession(null, null, "US", "en")
        override val configStore = object : VyonExConfigStore {
            override val enabled = false
            override fun getString(key: String): String? = null
            override fun getLong(key: String): Long? = null
            override fun putString(key: String, value: String) = Unit
            override fun putLong(key: String, value: Long) = Unit
        }
        override val tokenProvider: TokenProvider? = null
        override var filesDir: File? = null
        override val httpClient = OkHttpClient()
        override fun nowMs() = 0L
        override fun log(level: VyonExLogLevel, message: String, trackId: String?) = Unit
    }
}
