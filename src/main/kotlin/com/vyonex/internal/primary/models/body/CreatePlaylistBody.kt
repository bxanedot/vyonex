// bxane
package com.vyonex.internal.primary.models.body

import com.vyonex.internal.primary.models.Context
import kotlinx.serialization.Serializable

@Serializable
internal data class CreatePlaylistBody(
    val context: Context,
    val title: String,
    val privacyStatus: String,
    val videoIds: List<String>? = null,
)
