// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory;
import com.vyonex.internal.json.JsonObject;

import java.io.IOException;
import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getBannersFromAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.readApiObject;

public final class PeertubeChannelExtractor extends ChannelExtractor {
    private static final String API = "/api/v1/";
    private final String baseUrl;
    private String apiId;
    private String displayName;
    private String description;
    private long subscriberCount;
    private String ownerName;
    private String ownerUrl;
    private List<Image> avatars = List.of();
    private List<Image> banners = List.of();
    private List<Image> ownerAvatars = List.of();

    public PeertubeChannelExtractor(final StreamingService service, final ListLinkHandler link)
            throws ParsingException {
        super(service, link);
        baseUrl = getBaseUrl();
    }

    @Override public List<Image> getAvatars() {
        return avatars;
    }
    @Override public List<Image> getBanners() {
        return banners;
    }
    @Override public String getFeedUrl() { return baseUrl + "/feeds/videos.xml?videoChannelId=" + apiId; }
    @Override public long getSubscriberCount() { return subscriberCount; }
    @Override public String getDescription() { return description; }
    @Override public String getParentChannelName() throws ParsingException {
        return requiredStoredString(ownerName, "ownerAccount.name");
    }
    @Override public String getParentChannelUrl() throws ParsingException {
        return requiredStoredString(ownerUrl, "ownerAccount.url");
    }
    @Override public List<Image> getParentChannelAvatars() {
        return ownerAvatars;
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
        final JsonObject channel = readApiObject(downloader.get(baseUrl + API + getId()), "channel");
        PeertubeParsingHelper.validate(channel);
        apiId = String.valueOf(channel.get("id"));
        displayName = channel.getString("displayName");
        description = channel.getString("description");
        subscriberCount = channel.getLong("followersCount");
        final JsonObject owner = channel.getObject("ownerAccount");
        ownerName = owner.getString("name");
        ownerUrl = owner.getString("url");
        avatars = getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, channel);
        banners = getBannersFromAccountOrVideoChannelObject(baseUrl, channel);
        ownerAvatars = getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, owner);
    }

    @Override public String getName() throws ParsingException {
        return requiredStoredString(displayName, "displayName");
    }

    private static String requiredStoredString(final String value, final String field)
            throws ParsingException {
        if (value == null) throw new ParsingException("Missing string field: " + field);
        return value;
    }
}
