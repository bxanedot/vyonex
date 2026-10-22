// bxane
package com.vyonex.internal.primary.models.response

import kotlinx.serialization.Serializable

@Serializable
internal data class ImageUploadResponse(
    val encryptedBlobId: String,
)
