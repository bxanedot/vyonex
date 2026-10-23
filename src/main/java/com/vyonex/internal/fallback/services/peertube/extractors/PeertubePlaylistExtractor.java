// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.playlist.PlaylistExtractor;
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;
import com.vyonex.internal.json.JsonObject;

import java.io.IOException;
import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.COUNT_KEY;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.ITEMS_PER_PAGE;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.START_KEY;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.collectItemsFrom;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.readApiObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getThumbnailsFromPlaylistOrVideoItem;

public final class PeertubePlaylistExtractor extends PlaylistExtractor {
    private JsonObject playlist;

    public PeertubePlaylistExtractor(final StreamingService service, final ListLinkHandler handler) {
        super(service, handler);
    }

    @Override public List<Image> getThumbnails() throws ParsingException {
        return getThumbnailsFromPlaylistOrVideoItem(getBaseUrl(), playlist);
    }
    @Override public String getUploaderUrl() { return playlist.getObject("ownerAccount").getString("url"); }
    @Override public String getUploaderName() {
        return playlist.getObject("ownerAccount").getString("displayName");
    }
    @Override public List<Image> getUploaderAvatars() throws ParsingException {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(getBaseUrl(),
                playlist.getObject("ownerAccount"));
    }
    @Override public boolean isUploaderVerified() { return false; }
    @Override public long getStreamCount() { return playlist.getLong("videosLength", -1); }
    @Override public Description getDescription() {
        final String text = playlist.getString("description");
        return text == null || text.isEmpty()
                ? Description.EMPTY_DESCRIPTION : new Description(text, Description.PLAIN_TEXT);
    }
    @Override public String getSubChannelName() {
        return playlist.getObject("videoChannel").getString("displayName");
    }
    @Override public String getSubChannelUrl() {
        return playlist.getObject("videoChannel").getString("url");
    }
    @Override public List<Image> getSubChannelAvatars() throws ParsingException {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(getBaseUrl(),
                playlist.getObject("videoChannel"));
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        final String firstUrl = getUrl() + "/videos?" + START_KEY + "=0&"
                + COUNT_KEY + "=" + ITEMS_PER_PAGE;
        return getPage(new Page(firstUrl));
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            throw new IllegalArgumentException("Page URL is required");
        }
        final JsonObject data = readApiObject(getDownloader().get(page.getUrl()), "playlist videos");
        PeertubeParsingHelper.validate(data);
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        collectItemsFrom(items, data, getBaseUrl());
        final Page next = PeertubeParsingHelper.getNextPage(page.getUrl(), data.getLong("total"));
        return new InfoItemsPage<>(items, next);
    }

    @Override
    public void onFetchPage(final Downloader downloader) throws IOException, ExtractionException {
        playlist = readApiObject(downloader.get(getUrl()), "playlist");
        PeertubeParsingHelper.validate(playlist);
    }

    @Override public String getName() throws ParsingException {
        return PeertubeParsingHelper.requiredString(playlist, "displayName");
    }

}
