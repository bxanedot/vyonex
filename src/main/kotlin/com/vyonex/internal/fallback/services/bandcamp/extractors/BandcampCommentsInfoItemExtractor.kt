// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.comments.CommentsInfoItemExtractor
import com.vyonex.internal.fallback.stream.Description
import com.vyonex.internal.json.JsonObject

public class BandcampCommentsInfoItemExtractor(
    review: JsonObject,
    private val url: String?,
) : CommentsInfoItemExtractor {
    private val text = review.getString("why", "")
    private val uploaderName = review.getString("name", "")
    private val avatarId = review.getLong("image_id")

    override fun getName(): String = getCommentText().content

    override fun getUrl(): String? = url

    override fun getThumbnails(): List<Image> = getUploaderAvatars()

    override fun getCommentText(): Description = Description(text, Description.PLAIN_TEXT)

    override fun getUploaderName(): String = uploaderName

    override fun getUploaderAvatars(): List<Image> =
        BandcampExtractorHelper.getImagesFromImageId(avatarId, false)
}
