// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.models.body

import com.vyonex.internal.primary.models.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerBodyContractTest {
    @Test
    fun serializesPlaybackContextAndIntegrityToken() {
        val body =
            PlayerBody(
                context =
                    Context(
                        client =
                            Context.Client(
                                clientName = "WEB",
                                clientVersion = "1.0",
                                gl = "US",
                                hl = "en",
                                visitorData = "visitor",
                            ),
                    ),
                videoId = "abcdefghijk",
                playlistId = "playlist",
                playbackContext =
                    PlayerBody.PlaybackContext(
                        PlayerBody.PlaybackContext.ContentPlaybackContext(
                            html5Preference = "HTML5_PREF_WANTS",
                            signatureTimestamp = 12345,
                            encryptedHostFlags = "host-flags",
                        ),
                    ),
                thirdParty = PlayerBody.ThirdParty("https://www.youtube.com/embed/abcdefghijk"),
                serviceIntegrityDimensions = PlayerBody.ServiceIntegrityDimensions("po-token"),
                contentCheckOk = true,
                racyCheckOk = true,
                videoCheckOk = false,
            )
        val encoded = Json.encodeToJsonElement(PlayerBody.serializer(), body).jsonObject
        val playback = encoded["playbackContext"]!!.jsonObject["contentPlaybackContext"]!!.jsonObject

        assertEquals(JsonPrimitive("abcdefghijk"), encoded["videoId"])
        assertEquals(JsonPrimitive("playlist"), encoded["playlistId"])
        assertEquals(JsonPrimitive(12345), playback["signatureTimestamp"])
        assertEquals(JsonPrimitive("host-flags"), playback["encryptedHostFlags"])
        assertEquals(JsonPrimitive("po-token"), encoded["serviceIntegrityDimensions"]!!.jsonObject["poToken"])
        assertEquals(JsonPrimitive(true), encoded["contentCheckOk"])
        assertEquals(JsonPrimitive(true), encoded["racyCheckOk"])
        assertEquals(JsonPrimitive(false), encoded["videoCheckOk"])
    }
}
