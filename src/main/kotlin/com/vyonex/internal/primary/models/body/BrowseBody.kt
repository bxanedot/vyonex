// bxane
package com.vyonex.internal.primary.models.body

import com.vyonex.internal.primary.models.Context
import kotlinx.serialization.Serializable

@Serializable
internal data class BrowseBody(
    val context: Context,
    val browseId: String? = null,
    val params: String? = null,
    val continuation: String? = null,
)
