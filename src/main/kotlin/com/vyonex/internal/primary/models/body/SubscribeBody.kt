// bxane
package com.vyonex.internal.primary.models.body

import com.vyonex.internal.primary.models.Context
import kotlinx.serialization.Serializable

@Serializable
internal data class SubscribeBody(
    val context: Context,
    val channelIds: List<String>,
    val params: String? = null,
)
