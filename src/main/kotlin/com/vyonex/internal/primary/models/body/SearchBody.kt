// bxane
package com.vyonex.internal.primary.models.body

import com.vyonex.internal.primary.models.Context
import kotlinx.serialization.Serializable

@Serializable
internal data class SearchBody(
    val context: Context,
    val query: String? = null,
    val params: String? = null,
)
