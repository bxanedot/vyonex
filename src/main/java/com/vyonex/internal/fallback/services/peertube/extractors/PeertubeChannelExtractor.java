// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import java.io.IOException;
import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getBannersFromAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.requiredString;

public final class PeertubeChannelExtractor extends ChannelExtractor {
    private static final String API = "/api/v1/";
    private final String baseUrl;
    private JsonObject channel;

    public PeertubeChannelExtractor(final StreamingService service, final ListLinkHandler link)
            throws ParsingException {
        super(service, link);
        baseUrl = getBaseUrl();
    }

    @Override public List<Image> getAvatars() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, channel);
    }
    @Override public List<Image> getBanners() {
        return getBannersFromAccountOrVideoChannelObject(baseUrl, channel);
    }
    @Override public String getFeedUrl() { return baseUrl + "/feeds/videos.xml?videoChannelId=" + channel.get("id"); }
    @Override public long getSubscriberCount() { return channel.getLong("followersCount"); }
    @Override public String getDescription() { return channel.getString("description"); }
    @Override public String getParentChannelName() throws ParsingException {
        return requiredString(channel, "ownerAccount.name");
    }
    @Override public String getParentChannelUrl() throws ParsingException {
        return requiredString(channel, "ownerAccount.url");
    }
    @Override public List<Image> getParentChannelAvatars() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl,
                channel.getObject("ownerAccount"));
    }
    @Override public boolean isVerified() { return false; }

    @Override
    public List<ListLinkHandler> getTabs() throws ParsingException {
        final PeertubeChannelTabLinkHandlerFactory tabs =
                PeertubeChannelTabLinkHandlerFactory.getInstance();
        return List.of(tabs.fromQuery(getId(), List.of(ChannelTabs.VIDEOS), "", baseUrl),
                tabs.fromQuery(getId(), List.of(ChannelTabs.PLAYLISTS), "", baseUrl));
    }

    @Override
    public void onFetchPage(final Downloader downloader) throws IOException, ExtractionException {
        final Response response = downloader.get(baseUrl + API + getId());
        if (response == null || response.responseBody().isBlank()) {
            throw new ExtractionException("PeerTube channel response is empty");
        }
        try {
            channel = JsonParser.object().from(response.responseBody());
        } catch (final JsonParserException error) {
            throw new ExtractionException("PeerTube channel response is invalid", error);
        }
        PeertubeParsingHelper.validate(channel);
    }

    @Override public String getName() throws ParsingException {
        return requiredString(channel, "displayName");
    }
}
