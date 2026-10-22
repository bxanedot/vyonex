// bxane
package com.vyonex.internal.primary.models.body

import com.vyonex.internal.primary.models.Context
import kotlinx.serialization.Serializable

@Serializable
internal data class PlaylistDeleteBody(
    val context: Context,
    val playlistId: String,
)
