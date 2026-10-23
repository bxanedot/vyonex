// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.models.body

import com.vyonex.internal.primary.models.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

@Serializable
internal data class EditPlaylistBody(
    val context: Context,
    val playlistId: String,
    val actions: List<JsonObject>,
)

internal object Action {
    private fun create(action: String, fields: JsonObjectBuilder.() -> Unit = {}): JsonObject =
        buildJsonObject {
            put("action", action)
            fields()
        }

    fun addVideoAction(addedVideoId: String) = create("ACTION_ADD_VIDEO") { put("addedVideoId", addedVideoId) }

    fun addPlaylistAction(addedFullListId: String) = create("ACTION_ADD_PLAYLIST") { put("addedFullListId", addedFullListId) }

    fun moveVideoAction(setVideoId: String, movedSetVideoIdSuccessor: String?) =
        create("ACTION_MOVE_VIDEO_BEFORE") {
            put("setVideoId", setVideoId)
            put("movedSetVideoIdSuccessor", movedSetVideoIdSuccessor)
        }

    fun removeVideoAction(setVideoId: String, removedVideoId: String) =
        create("ACTION_REMOVE_VIDEO") {
            put("setVideoId", setVideoId)
            put("removedVideoId", removedVideoId)
        }

    fun renamePlaylistAction(playlistName: String) = create("ACTION_SET_PLAYLIST_NAME") { put("playlistName", playlistName) }

    fun setPlaylistDescriptionAction(playlistDescription: String) =
        create("ACTION_SET_PLAYLIST_DESCRIPTION") { put("playlistDescription", playlistDescription) }

    fun setCustomThumbnailAction(encryptedBlobId: String) =
        create("ACTION_SET_CUSTOM_THUMBNAIL") {
            putJsonObject("addedCustomThumbnail") {
                put("playlistScottyEncryptedBlobId", encryptedBlobId)
            }
        }

    fun removeCustomThumbnailAction() = create("ACTION_REMOVE_CUSTOM_THUMBNAIL")
}
