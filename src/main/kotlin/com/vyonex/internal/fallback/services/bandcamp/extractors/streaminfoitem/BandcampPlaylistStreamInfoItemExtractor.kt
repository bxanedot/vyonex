// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.exceptions.ParsingException
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

public class BandcampPlaylistStreamInfoItemExtractor(
    private val title: String?,
    private val titleLink: String?,
    private val duration: Long,
    private val artist: String,
    uploaderUrl: String?,
    private val service: StreamingService?,
    substituteCovers: List<Image>,
) : BandcampStreamInfoItemExtractor(uploaderUrl) {
    private val substituteCovers = java.util.List.copyOf(substituteCovers)

    @Throws(ParsingException::class)
    override fun getName(): String = title ?: throw ParsingException("Bandcamp track has no title")

    @Throws(ParsingException::class)
    override fun getUrl(): String? {
        if (titleLink.isNullOrBlank()) return null
        return try {
            val baseUrl = getUploaderUrl() ?: throw URISyntaxException("", "Missing artist URL")
            URI(baseUrl).resolve(titleLink).toString().replaceFirst("^http://", "https://")
        } catch (error: URISyntaxException) {
            throw ParsingException("Bandcamp track has an invalid URL", error)
        }
    }

    override fun getDuration(): Long = duration

    override fun getUploaderName(): String = artist

    @Throws(ParsingException::class)
    override fun getThumbnails(): List<Image> {
        if (substituteCovers.isNotEmpty()) return substituteCovers
        val trackUrl = getUrl() ?: return emptyList()
        val extractorService = service ?: return emptyList()
        try {
            val extractor = extractorService.getStreamExtractor(trackUrl)
            extractor.fetchPage()
            return extractor.thumbnails
        } catch (error: ExtractionException) {
            throw ParsingException("Could not load the track cover", error)
        } catch (error: IOException) {
            throw ParsingException("Could not load the track cover", error)
        }
    }
}
