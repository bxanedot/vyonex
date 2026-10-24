// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors

import com.vyonex.internal.fallback.Image
import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.channel.ChannelExtractor
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler
import com.vyonex.internal.fallback.linkhandler.ReadyChannelTabListLinkHandler
import com.vyonex.internal.fallback.services.media_ccc.extractors.infoItems.MediaCCCStreamInfoItemExtractor
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCConferencePayload
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCListingPayloadDecoder
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCListingParseException

public class MediaCCCConferenceExtractor(
    service: StreamingService,
    linkHandler: ListLinkHandler,
) : ChannelExtractor(service, linkHandler) {
    private lateinit var conference: MediaCCCConferenceData

    override fun getAvatars(): List<Image> =
        MediaCCCParsingHelper.getImageListFromLogoImageUrl(conference.logoUrl)

    override fun getBanners(): List<Image> = emptyList()

    override fun getFeedUrl(): String? = null

    override fun getSubscriberCount(): Long = -1

    override fun getDescription(): String? = null

    override fun getParentChannelName(): String = ""

    override fun getParentChannelUrl(): String = ""

    override fun getParentChannelAvatars(): List<Image> = emptyList()

    override fun isVerified(): Boolean = false

    override fun getTabs(): List<ListLinkHandler> = listOf(
        ReadyChannelTabListLinkHandler(url, id, ChannelTabs.VIDEOS) { tabService, link ->
            MediaCCCChannelTabExtractor(tabService, link, conference.events)
        },
    )

    override fun onFetchPage(downloader: Downloader) {
        conference = fetchMediaCCCConferenceData(downloader, id)
    }

    override fun getName(): String = conference.title.orEmpty()
}

internal data class MediaCCCConferenceData(
    val title: String?,
    val logoUrl: String?,
    val events: List<MediaCCCStreamInfoItemExtractor>,
)

internal fun fetchMediaCCCConferenceData(
    downloader: Downloader,
    conferenceId: String,
): MediaCCCConferenceData {
    val url = MediaCCCConferenceLinkHandlerFactory.CONFERENCE_API_ENDPOINT + conferenceId
    val payload = try {
        MediaCCCListingPayloadDecoder.decodeConference(
            MediaCCCParsingHelper.fetchJsonBody(downloader, url, null),
        )
    } catch (error: MediaCCCListingParseException) {
        throw ExtractionException("Could not parse API response from $url", error)
    }
    val events = payload.events.map(::MediaCCCStreamInfoItemExtractor)
    return MediaCCCConferenceData(payload.title, payload.logoUrl, events)
}
