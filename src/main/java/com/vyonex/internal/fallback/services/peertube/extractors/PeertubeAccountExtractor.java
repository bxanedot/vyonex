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
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import java.io.IOException;
import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getBannersFromAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.requiredString;

public final class PeertubeAccountExtractor extends ChannelExtractor {
    private static final String API = "/api/v1/";
    private final String baseUrl;
    private JsonObject account;

    public PeertubeAccountExtractor(final StreamingService service, final ListLinkHandler link)
            throws ParsingException {
        super(service, link);
        baseUrl = getBaseUrl();
    }

    @Override public List<Image> getAvatars() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, account);
    }
    @Override public List<Image> getBanners() {
        return getBannersFromAccountOrVideoChannelObject(baseUrl, account);
    }
    @Override public String getFeedUrl() { return baseUrl + "/feeds/videos.xml?accountId=" + account.get("id"); }
    @Override public String getDescription() { return account.getString("description"); }
    @Override public String getParentChannelName() { return ""; }
    @Override public String getParentChannelUrl() { return ""; }
    @Override public List<Image> getParentChannelAvatars() { return List.of(); }
    @Override public boolean isVerified() { return false; }

    @Override
    public long getSubscriberCount() throws ParsingException {
        long followers = account.getLong("followersCount");
        final String channelListUrl = baseUrl + API + getId() + "/video-channels";
        try {
            final Response response = getDownloader().get(channelListUrl);
            if (response != null && !response.responseBody().isBlank()) {
                final JsonObject result = JsonParser.object().from(response.responseBody());
                final JsonArray channels = result.getArray("data");
                for (final Object value : channels) {
                    if (value instanceof JsonObject) {
                        followers += ((JsonObject) value).getLong("followersCount");
                    }
                }
            }
        } catch (final Exception ignored) {
            // Keep the account total when its optional channel list cannot be read.
        }
        return followers;
    }

    @Override
    public List<ListLinkHandler> getTabs() throws ParsingException {
        final PeertubeChannelTabLinkHandlerFactory tabs =
                PeertubeChannelTabLinkHandlerFactory.getInstance();
        return List.of(tabs.fromQuery(getId(), List.of(ChannelTabs.VIDEOS), "", baseUrl),
                tabs.fromQuery(getId(), List.of(ChannelTabs.CHANNELS), "", baseUrl));
    }

    @Override
    public void onFetchPage(final Downloader downloader) throws IOException, ExtractionException {
        final Response response = downloader.get(baseUrl + API + getId());
        if (response == null || response.responseBody().isBlank()) {
            throw new ExtractionException("PeerTube account response is empty");
        }
        try {
            account = JsonParser.object().from(response.responseBody());
        } catch (final JsonParserException error) {
            throw new ExtractionException("PeerTube account response is invalid", error);
        }
        PeertubeParsingHelper.validate(account);
    }

    @Override public String getName() throws ParsingException {
        return requiredString(account, "displayName");
    }
}
