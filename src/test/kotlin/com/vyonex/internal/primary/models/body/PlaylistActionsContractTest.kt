// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.models.body

import com.vyonex.internal.primary.models.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaylistActionsContractTest {
    @Test
    fun encodesPlaylistActionPayloads() {
        assertAction(Action.addVideoAction("video"), """{"action":"ACTION_ADD_VIDEO","addedVideoId":"video"}""")
        assertAction(Action.addPlaylistAction("playlist"), """{"action":"ACTION_ADD_PLAYLIST","addedFullListId":"playlist"}""")
        assertAction(
            Action.moveVideoAction("set-video", null),
            """{"action":"ACTION_MOVE_VIDEO_BEFORE","setVideoId":"set-video","movedSetVideoIdSuccessor":null}""",
        )
        assertAction(
            Action.removeVideoAction("set-video", "video"),
            """{"action":"ACTION_REMOVE_VIDEO","setVideoId":"set-video","removedVideoId":"video"}""",
        )
        assertAction(Action.renamePlaylistAction("title"), """{"action":"ACTION_SET_PLAYLIST_NAME","playlistName":"title"}""")
        assertAction(
            Action.setPlaylistDescriptionAction("description"),
            """{"action":"ACTION_SET_PLAYLIST_DESCRIPTION","playlistDescription":"description"}""",
        )
        assertAction(
            Action.setCustomThumbnailAction("blob"),
            """{"action":"ACTION_SET_CUSTOM_THUMBNAIL","addedCustomThumbnail":{"playlistScottyEncryptedBlobId":"blob"}}""",
        )
        assertAction(Action.removeCustomThumbnailAction(), """{"action":"ACTION_REMOVE_CUSTOM_THUMBNAIL"}""")
    }

    @Test
    fun serializesThePlaylistEditEnvelope() {
        val body = EditPlaylistBody(context(), "playlist", listOf(Action.addVideoAction("video")))
        val encoded = Json.encodeToJsonElement(EditPlaylistBody.serializer(), body).jsonObject

        assertEquals(JsonPrimitive("playlist"), encoded["playlistId"])
        assertEquals(
            Json.parseToJsonElement("""[{"action":"ACTION_ADD_VIDEO","addedVideoId":"video"}]""").jsonArray,
            encoded["actions"]?.jsonArray,
        )
    }

    private fun assertAction(action: JsonObject, expected: String) {
        assertEquals(Json.parseToJsonElement(expected), action)
    }

    private fun context() =
        Context(
            client =
                Context.Client(
                    clientName = "WEB",
                    clientVersion = "1.0",
                    gl = "US",
                    hl = "en",
                    visitorData = null,
                ),
        )
}
