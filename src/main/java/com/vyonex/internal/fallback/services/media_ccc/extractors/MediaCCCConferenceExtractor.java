// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.linkhandler.ReadyChannelTabListLinkHandler;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.extractors.infoItems.MediaCCCStreamInfoItemExtractor;
import com.vyonex.internal.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper.getImageListFromLogoImageUrl;

public class MediaCCCConferenceExtractor extends ChannelExtractor {
    private ConferenceData conference;

    public MediaCCCConferenceExtractor(final StreamingService service,
                                       final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    static ConferenceData fetchConferenceData(@Nonnull final Downloader downloader,
                                              @Nonnull final String conferenceId)
            throws IOException, ExtractionException {
        final String url = MediaCCCConferenceLinkHandlerFactory.CONFERENCE_API_ENDPOINT
                + conferenceId;
        final JsonObject payload = MediaCCCParsingHelper.fetchJsonObject(downloader, url, null);
        final List<MediaCCCStreamInfoItemExtractor> events = payload.getArray("events")
                .streamAsJsonObjects().map(MediaCCCStreamInfoItemExtractor::new).toList();
        return new ConferenceData(payload.getString("title"), payload.getString("logo_url"),
                events);
    }

    @Nonnull @Override public List<Image> getAvatars() {
        return getImageListFromLogoImageUrl(conference.logoUrl());
    }
    @Nonnull @Override public List<Image> getBanners() { return List.of(); }
    @Override public String getFeedUrl() { return null; }
    @Override public long getSubscriberCount() { return -1; }
    @Override public String getDescription() { return null; }
    @Override public String getParentChannelName() { return ""; }
    @Override public String getParentChannelUrl() { return ""; }
    @Nonnull @Override public List<Image> getParentChannelAvatars() { return List.of(); }
    @Override public boolean isVerified() { return false; }

    @Nonnull @Override public List<ListLinkHandler> getTabs() throws ParsingException {
        final List<MediaCCCStreamInfoItemExtractor> fetched = conference.events();
        return List.of(new ReadyChannelTabListLinkHandler(getUrl(), getId(), ChannelTabs.VIDEOS,
                (service, link) -> new MediaCCCChannelTabExtractor(service, link, fetched)));
    }

    @Override public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        conference = fetchConferenceData(downloader, getId());
    }

    @Nonnull @Override public String getName() throws ParsingException {
        return conference.title();
    }

    record ConferenceData(@Nullable String title, @Nullable String logoUrl,
                         List<MediaCCCStreamInfoItemExtractor> events) { }
}
