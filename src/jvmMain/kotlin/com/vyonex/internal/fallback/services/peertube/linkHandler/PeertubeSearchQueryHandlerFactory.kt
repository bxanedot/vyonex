// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler

import com.vyonex.internal.fallback.ServiceList
import com.vyonex.internal.fallback.exceptions.ParsingException
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory

public class PeertubeSearchQueryHandlerFactory private constructor() : SearchQueryHandlerFactory() {
    @Throws(ParsingException::class)
    override fun getUrl(query: String, filters: List<String>, sortFilter: String): String {
        val remote = filters.firstOrNull()?.startsWith("sepia_") == true
        val baseUrl = if (remote) SEPIA_BASE_URL else ServiceList.PeerTube.getBaseUrl()
        return getUrl(query, filters, sortFilter, baseUrl)
    }

    override fun getUrl(
        query: String,
        filters: List<String>,
        sortFilter: String,
        baseUrl: String,
    ): String {
        val endpoint = when (filters.firstOrNull() ?: VIDEOS) {
            VIDEOS, SEPIA_VIDEOS -> SEARCH_ENDPOINT_VIDEOS
            CHANNELS -> SEARCH_ENDPOINT_CHANNELS
            PLAYLISTS -> SEARCH_ENDPOINT_PLAYLISTS
            else -> SEARCH_ENDPOINT_PLAYLISTS
        }
        return "$baseUrl$endpoint?search=${PeertubeUrl.encodeSearchQuery(query)}"
    }

    override fun getAvailableContentFilter(): Array<String> =
        arrayOf(VIDEOS, PLAYLISTS, CHANNELS, SEPIA_VIDEOS)

    public companion object {
        public const val VIDEOS = "videos"
        public const val SEPIA_VIDEOS = "sepia_videos"
        public const val PLAYLISTS = "playlists"
        public const val CHANNELS = "channels"
        public const val SEPIA_BASE_URL = "https://sepiasearch.org"
        public const val SEARCH_ENDPOINT_PLAYLISTS = "/api/v1/search/video-playlists"
        public const val SEARCH_ENDPOINT_VIDEOS = "/api/v1/search/videos"
        public const val SEARCH_ENDPOINT_CHANNELS = "/api/v1/search/video-channels"
        private val INSTANCE = PeertubeSearchQueryHandlerFactory()

        @JvmStatic
        public fun getInstance(): PeertubeSearchQueryHandlerFactory = INSTANCE
    }
}
