// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem

import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.localization.DateWrapper
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor
import com.vyonex.internal.fallback.stream.StreamType

public abstract class BandcampStreamInfoItemExtractor protected constructor(
    private val uploaderUrl: String?,
) : StreamInfoItemExtractor {
    override fun getStreamType(): StreamType = StreamType.AUDIO_STREAM

    override fun getViewCount(): Long = -1

    override fun getUploaderUrl(): String? = uploaderUrl

    override fun getTextualUploadDate(): String? = null

    override fun getUploadDate(): DateWrapper? = null

    @Throws(ParsingException::class)
    override fun isUploaderVerified(): Boolean = false

    override fun isAd(): Boolean = false
}
