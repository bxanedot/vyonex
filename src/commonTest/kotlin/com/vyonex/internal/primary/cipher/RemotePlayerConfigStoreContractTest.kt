// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.cipher

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class RemotePlayerConfigStoreContractTest {
    @Test
    fun loadsValidatedPlayerConfigsFromTheConfiguredSource() = runTest {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requests += request.url.toString()
            respond(
                content = ByteReadChannel(CONFIG_JSON),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        try {
            val config = RemotePlayerConfigStore(client, TestRepository()).getConfig(PLAYER_URL)

            assertEquals(12345, config?.signatureTimestamp)
            assertEquals(listOf(DEFAULT_PLAYER_CONFIG_URL), requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun refusesConfigSourcesOutsideTheVyonExEndpoint() {
        val client = HttpClient(MockEngine { error("No request expected") })
        try {
            val store = RemotePlayerConfigStore(client, TestRepository())

            assertNull(store.validatedSourceUrlOrNull("https://raw.githubusercontent.com/attacker/repo/main/player_configs.json"))
        } finally {
            client.close()
        }
    }

    private class TestRepository : PlayerConfigRepository {
        override val enabled = true
        override val sourceUrl = DEFAULT_PLAYER_CONFIG_URL
        override var cachedJson = ""
        override var cachedAtMs = 0L
        override var cachedSourceUrl = ""
        override var cachedEtag = ""
    }

    private companion object {
        const val PLAYER_URL = "https://www.youtube.com/s/player/1234abcd/player_ias.vflset/en_US/base.js"
        const val CONFIG_JSON =
            """{"schemaVersion":1,"players":{"1234abcd":{"sig":"x(1,2,INPUT)","nClass":"A","sts":12345}}}"""
    }
}
