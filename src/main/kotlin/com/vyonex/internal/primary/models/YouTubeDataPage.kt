// bxane
package com.vyonex.internal.primary.models

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeDataPage<T>(
    val items: List<T>,
    val continuation: String? = null,
)
