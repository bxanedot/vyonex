package com.vyonex
// bxane

import kotlin.test.Test
import kotlin.test.assertNull

class VyonExIosApiTest {
    @Test
    fun primaryFacadeCanBeConstructedWithoutJvmTransportTypes() {
        val extractor = VyonEx(TestHost())

        assertNull(extractor.headersFor("https://audio.invalid/stream"))
        assertNull(extractor.onRefused("https://audio.invalid/stream"))
    }

    private class TestHost : VyonExHost {
        override val tokenProvider = null
        override val configStore = TestConfigStore()

        override suspend fun session() = VyonExSession(null, null, "US", "en")

        override fun nowMs() = 1L

        override fun log(level: VyonExLogLevel, message: String, trackId: String?) = Unit
    }

    private class TestConfigStore : VyonExConfigStore {
        private val strings = mutableMapOf<String, String>()
        private val longs = mutableMapOf<String, Long>()

        override val enabled = false

        override fun getString(key: String) = strings[key]
        override fun getLong(key: String) = longs[key]
        override fun putString(key: String, value: String) { strings[key] = value }
        override fun putLong(key: String, value: Long) { longs[key] = value }
    }
}
