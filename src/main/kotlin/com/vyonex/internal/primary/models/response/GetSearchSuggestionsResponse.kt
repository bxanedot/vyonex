// bxane
package com.vyonex.internal.primary.models.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GetSearchSuggestionsResponse(
    val contents: List<JsonObject>? = null,
)
