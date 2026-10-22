// bxane
package com.vyonex.internal.primary.models.body

import com.vyonex.internal.primary.models.Context
import kotlinx.serialization.Serializable

@Serializable
internal data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)
