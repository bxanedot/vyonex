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
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

import static com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper
        .getImageListFromLogoImageUrl;

public class MediaCCCConferenceExtractor extends ChannelExtractor {
    private JsonObject data;

    public MediaCCCConferenceExtractor(final StreamingService service,
                                       final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    static JsonObject fetchConferenceData(@Nonnull final Downloader downloader,
                                          @Nonnull final String conferenceId)
            throws IOException, ExtractionException {
        final String url = MediaCCCConferenceLinkHandlerFactory.CONFERENCE_API_ENDPOINT
                + conferenceId;
        try {
            return JsonParser.object().from(downloader.get(url).responseBody());
        } catch (JsonParserException e) {
            throw new ExtractionException("Could not parse conference response from " + url, e);
        }
    }

    @Nonnull
    @Override
    public List<Image> getAvatars() { return getImageListFromLogoImageUrl(data.getString("logo_url")); }

    @Nonnull
    @Override
    public List<Image> getBanners() { return List.of(); }

    @Override
    public String getFeedUrl() { return null; }

    @Override
    public long getSubscriberCount() { return -1; }

    @Override
    public String getDescription() { return null; }

    @Override
    public String getParentChannelName() { return ""; }

    @Override
    public String getParentChannelUrl() { return ""; }

    @Nonnull
    @Override
    public List<Image> getParentChannelAvatars() { return List.of(); }

    @Override
    public boolean isVerified() { return false; }

    @Nonnull
    @Override
    public List<ListLinkHandler> getTabs() throws ParsingException {
        final JsonObject fetchedConference = data;
        return List.of(new ReadyChannelTabListLinkHandler(getUrl(), getId(), ChannelTabs.VIDEOS,
                (service, linkHandler) ->
                        new MediaCCCChannelTabExtractor(service, linkHandler, fetchedConference)));
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        data = fetchConferenceData(downloader, getId());
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException { return data.getString("title"); }
}
