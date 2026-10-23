// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudChannelTabLinkHandlerFactory;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SoundcloudChannelExtractor extends ChannelExtractor {
    private String userId;
    private String name;
    private String avatarUrl;
    private List<String> visualUrls = List.of();
    private long subscriberCount;
    private String description;
    private boolean verified;

    public SoundcloudChannelExtractor(final StreamingService service, final ListLinkHandler handler) {
        super(service, handler);
    }

    @Override public void onFetchPage(final Downloader downloader)
            throws IOException, ExtractionException {
        userId = getLinkHandler().getId();
        final String url = SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL + "users/" + userId
                + "?client_id=" + SoundcloudParsingHelper.clientId();
        final Response response = downloader.get(url, getExtractorLocalization());
        if (response.responseCode() >= 400) {
            throw new IOException("SoundCloud user request failed with HTTP " + response.responseCode());
        }
        try {
            final JsonObject user = JsonParser.object().from(response.responseBody());
            name = user.getString("username", "");
            avatarUrl = user.getString("avatar_url");
            subscriberCount = user.getLong("followers_count");
            description = user.getString("description", "");
            verified = user.getBoolean("verified");
            final List<String> visuals = new ArrayList<>();
            for (final Object item : user.getObject("visuals").getArray("visuals")) {
                if (item instanceof JsonObject) {
                    final String visualUrl = ((JsonObject) item).getString("visual_url");
                    if (visualUrl != null) visuals.add(visualUrl);
                }
            }
            visualUrls = List.copyOf(visuals);
        } catch (final JsonParserException e) {
            throw new ParsingException("Invalid SoundCloud user response", e);
        }
    }

    @Override public String getId() { return userId; }
    @Override public String getName() { return name; }
    @Override public List<Image> getAvatars() {
        return SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl(avatarUrl);
    }
    @Override public List<Image> getBanners() {
        final List<Image> images = new ArrayList<>();
        for (final String url : visualUrls) {
            images.addAll(SoundcloudParsingHelper.getAllImagesFromVisualUrl(url));
        }
        return images;
    }
    @Override public String getFeedUrl() { return null; }
    @Override public long getSubscriberCount() { return subscriberCount; }
    @Override public String getDescription() { return description; }
    @Override public String getParentChannelName() { return ""; }
    @Override public String getParentChannelUrl() { return ""; }
    @Override public List<Image> getParentChannelAvatars() { return List.of(); }
    @Override public boolean isVerified() { return verified; }

    @Override public List<ListLinkHandler> getTabs() throws ParsingException {
        final String base = getUrl();
        final String id = getId();
        final SoundcloudChannelTabLinkHandlerFactory tabs =
                SoundcloudChannelTabLinkHandlerFactory.getInstance();
        return List.of(tab(tabs, base, id, ChannelTabs.TRACKS),
                tab(tabs, base, id, ChannelTabs.PLAYLISTS),
                tab(tabs, base, id, ChannelTabs.ALBUMS),
                tab(tabs, base, id, ChannelTabs.LIKES));
    }

    private static ListLinkHandler tab(final SoundcloudChannelTabLinkHandlerFactory factory,
                                       final String base, final String id, final String tab) {
        final String url = base + SoundcloudChannelTabLinkHandlerFactory.getUrlSuffix(tab);
        return new ListLinkHandler(url, url, id, List.of(tab), "");
    }
}
