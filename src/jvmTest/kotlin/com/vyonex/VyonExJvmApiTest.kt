/*
 * VyonEx — by bxane
 * Copyright (c) 2026 bxane
 */
package com.vyonex

import kotlin.test.Test
import kotlin.test.assertTrue

class VyonExJvmApiTest {
    @Test
    fun facadeRetainsItsPublicOperations() {
        val names = VyonEx::class.java.methods.map { it.name }.toSet()

        assertTrue(names.containsAll(setOf(
            "init",
            "initializeFallback",
            "audioCandidates",
            "extract",
            "headersFor",
            "onRefused",
            "exclude",
            "onSessionChanged",
        )))
    }

    @Suppress("unused")
    private suspend fun compilePublicContract(
        extractor: VyonEx,
        host: VyonExHost,
        store: VyonExConfigStore,
        stream: VyonExStream,
        candidate: VyonExAudioCandidate,
        format: VyonExAudioFormat,
    ) {
        host.session()
        host.configStore
        host.tokenProvider
        host.filesDir
        host.httpClient
        host.nowMs()
        host.log(VyonExLogLevel.INFO, "test", "video")
        store.enabled
        store.getString("key")
        store.getLong("key")
        store.putString("key", "value")
        store.putLong("key", 1L)
        stream.videoId
        stream.url
        stream.kbps
        stream.mimeType
        stream.loudnessDb
        stream.clientName
        stream.profileId
        stream.headers
        candidate.url
        candidate.kbps
        candidate.mimeType
        candidate.format
        format.name
        extractor.init()
        extractor.initializeFallback()
        extractor.audioCandidates("video")
        extractor.extract("video", 128, emptySet(), false)
        extractor.headersFor("https://media.example/audio")
        extractor.onRefused("https://media.example/audio")
        extractor.exclude("video", "profile")
        extractor.onSessionChanged()
    }
}
