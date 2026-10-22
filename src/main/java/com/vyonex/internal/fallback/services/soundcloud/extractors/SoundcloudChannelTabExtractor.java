// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public final class SoundcloudChannelTabExtractor extends ChannelTabExtractor {
    private static final int EMPTY_PAGE_LIMIT = 3;
    private final String userId;

    public SoundcloudChannelTabExtractor(final StreamingService service,
                                         final ListLinkHandler handler) {
        super(service, handler);
        userId = handler.getId();
    }

    @Override public void onFetchPage(final Downloader downloader) { }
    @Override public String getId() { return userId; }

    @Override public InfoItemsPage<InfoItem> getInitialPage()
            throws IOException, ExtractionException {
        final String endpoint = switch (getName()) {
            case ChannelTabs.TRACKS -> "tracks";
            case ChannelTabs.PLAYLISTS -> "playlists_without_albums";
            case ChannelTabs.ALBUMS -> "albums";
            case ChannelTabs.LIKES -> "likes";
            default -> throw new ParsingException("Unsupported SoundCloud tab: " + getName());
        };
        final String url = SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL + "users/" + userId
                + "/" + endpoint + "?client_id=" + SoundcloudParsingHelper.clientId()
                + "&limit=20&linked_partitioning=1";
        return readPages(url);
    }

    @Override public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            throw new IllegalArgumentException("Page URL is required");
        }
        return readPages(page.getUrl());
    }

    private InfoItemsPage<InfoItem> readPages(final String firstUrl)
            throws IOException, ExtractionException {
        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());
        final Set<String> visited = new HashSet<>();
        String current = firstUrl;
        String next = "";
        int emptyPages = 0;
        while (current != null && !current.isBlank()) {
            if (!visited.add(current)) { next = ""; break; }
            final int previousSize = items.getItems().size();
            next = SoundcloudParsingHelper.getInfoItemsFromApi(items, current);
            if (items.getItems().size() > previousSize) break;
            if (next.isBlank() || ++emptyPages >= EMPTY_PAGE_LIMIT) {
                next = "";
                break;
            }
            current = next;
        }
        return new InfoItemsPage<>(items, next.isBlank() ? null : new Page(next));
    }
}
