// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.playlist.PlaylistExtractor;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class YoutubeChannelTabPlaylistExtractor extends ChannelTabExtractor {
    private final PlaylistExtractor playlist;
    private boolean available;

    public YoutubeChannelTabPlaylistExtractor(@Nonnull final StreamingService service,
                                              @Nonnull final ListLinkHandler linkHandler)
            throws IllegalArgumentException, SystemPlaylistUrlCreationException {
        super(service, linkHandler);
        playlist = new YoutubePlaylistExtractor(service, systemPlaylistLink(linkHandler));
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        available = false;
        try {
            playlist.onFetchPage(downloader);
            available = true;
        } catch (final ContentNotAvailableException ignored) {
            // An empty channel tab has no backing system playlist.
        }
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        return available ? asInfoItemsPage(playlist.getInitialPage()) : InfoItemsPage.emptyPage();
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        return available ? asInfoItemsPage(playlist.getPage(page)) : InfoItemsPage.emptyPage();
    }

    private static InfoItemsPage<InfoItem> asInfoItemsPage(
            final InfoItemsPage<? extends InfoItem> page) {
        return new InfoItemsPage<>(new ArrayList<InfoItem>(page.getItems()), page.getNextPage(),
                page.getErrors());
    }

    static String systemPlaylistId(final String channelId, final String tab) {
        if (channelId == null || !channelId.startsWith("UC")) {
            throw new IllegalArgumentException("Invalid channel ID");
        }

        final String prefix;
        if (ChannelTabs.VIDEOS.equals(tab)) {
            prefix = "UULF";
        } else if (ChannelTabs.SHORTS.equals(tab)) {
            prefix = "UUSH";
        } else if (ChannelTabs.LIVESTREAMS.equals(tab)) {
            prefix = "UULV";
        } else {
            throw new IllegalArgumentException(
                    "Only Videos, Shorts and Livestreams tabs can extracted as playlists");
        }
        return prefix + channelId.substring(2);
    }

    private static ListLinkHandler systemPlaylistLink(final ListLinkHandler channelTab) {
        final List<String> filters = channelTab.getContentFilters();
        if (filters.isEmpty()) {
            throw new IllegalArgumentException("A content filter is required");
        }

        final String id = systemPlaylistId(channelTab.getId(), filters.get(0));
        try {
            final String url = YoutubePlaylistLinkHandlerFactory.getInstance().getUrl(id);
            return new ListLinkHandler(url, url, id, List.of(), "");
        } catch (final ParsingException e) {
            throw new SystemPlaylistUrlCreationException(
                    "Could not create a YouTube playlist from a valid playlist ID", e);
        }
    }

    public static final class SystemPlaylistUrlCreationException extends RuntimeException {
        SystemPlaylistUrlCreationException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
