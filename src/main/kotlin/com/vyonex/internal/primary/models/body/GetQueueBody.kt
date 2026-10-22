// bxane
package com.vyonex.internal.primary.models.body

import com.vyonex.internal.primary.models.Context
import kotlinx.serialization.Serializable

@Serializable
internal data class GetQueueBody(
    val context: Context,
    val videoIds: List<String>?,
    val playlistId: String?,
)
