// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
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

import javax.annotation.Nullable;
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
    @Nullable private Playlist playlist;

    public PeertubePlaylistExtractor(final StreamingService service, final ListLinkHandler handler) {
        super(service, handler);
    }

    @Override public List<Image> getThumbnails() throws ParsingException {
        return playlist.thumbnails();
    }
    @Override public String getUploaderUrl() { return playlist.ownerUrl(); }
    @Override public String getUploaderName() { return playlist.ownerName(); }
    @Override public List<Image> getUploaderAvatars() { return playlist.ownerAvatars(); }
    @Override public boolean isUploaderVerified() { return false; }
    @Override public long getStreamCount() { return playlist.videoCount(); }
    @Override public Description getDescription() {
        final String text = playlist.description();
        return text == null || text.isEmpty()
                ? Description.EMPTY_DESCRIPTION : new Description(text, Description.PLAIN_TEXT);
    }
    @Override public String getSubChannelName() { return playlist.channelName(); }
    @Override public String getSubChannelUrl() { return playlist.channelUrl(); }
    @Override public List<Image> getSubChannelAvatars() { return playlist.channelAvatars(); }

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
        final JsonObject data = readApiObject(downloader.get(getUrl()), "playlist");
        PeertubeParsingHelper.validate(data);
        final JsonObject owner = data.getObject("ownerAccount");
        final JsonObject channel = data.getObject("videoChannel");
        playlist = new Playlist(data.getString("displayName"), data.getString("description"),
                data.getLong("videosLength", -1), owner.getString("url"),
                owner.getString("displayName"), channel.getString("displayName"),
                channel.getString("url"), getThumbnailsFromPlaylistOrVideoItem(getBaseUrl(), data),
                getAvatarsFromOwnerAccountOrVideoChannelObject(getBaseUrl(), owner),
                getAvatarsFromOwnerAccountOrVideoChannelObject(getBaseUrl(), channel));
    }

    @Override public String getName() throws ParsingException {
        if (playlist.displayName() == null) throw new ParsingException("Missing string field: displayName");
        return playlist.displayName();
    }

    private record Playlist(@Nullable String displayName, @Nullable String description,
                            long videoCount, @Nullable String ownerUrl,
                            @Nullable String ownerName, @Nullable String channelName,
                            @Nullable String channelUrl, List<Image> thumbnails,
                            List<Image> ownerAvatars, List<Image> channelAvatars) { }

}
