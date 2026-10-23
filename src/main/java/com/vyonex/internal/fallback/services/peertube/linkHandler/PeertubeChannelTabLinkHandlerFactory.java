// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.linkHandler;

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.UnsupportedTabException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import javax.annotation.Nonnull;
import java.util.List;

public final class PeertubeChannelTabLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final PeertubeChannelTabLinkHandlerFactory INSTANCE =
            new PeertubeChannelTabLinkHandlerFactory();

    private PeertubeChannelTabLinkHandlerFactory() { }
    public static PeertubeChannelTabLinkHandlerFactory getInstance() { return INSTANCE; }

    @Nonnull
    public static String getUrlSuffix(@Nonnull final String tab) throws UnsupportedTabException {
        return switch (tab) {
            case ChannelTabs.VIDEOS -> "/videos";
            case ChannelTabs.CHANNELS -> "/video-channels";
            case ChannelTabs.PLAYLISTS -> "/video-playlists";
            default -> throw new UnsupportedTabException(tab);
        };
    }

    @Override public String getId(final String url) throws ParsingException {
        return PeertubeChannelLinkHandlerFactory.getInstance().getId(url);
    }
    @Override public String getUrl(final String id, final List<String> filters, final String sort)
            throws ParsingException {
        return getUrl(id, filters, sort, null);
    }
    @Override
    public String getUrl(final String id, final List<String> filters, final String sort,
                         final String baseUrl) throws ParsingException {
        if (filters.isEmpty()) throw new ParsingException("PeerTube channel tab is missing");
        final PeertubeChannelLinkHandlerFactory channels =
                PeertubeChannelLinkHandlerFactory.getInstance();
        final String root = baseUrl == null ? channels.getUrl(id)
                : channels.getUrl(id, filters, sort, baseUrl);
        return root + getUrlSuffix(filters.get(0));
    }
    @Override public boolean onAcceptUrl(final String url) {
        return PeertubeChannelLinkHandlerFactory.getInstance().onAcceptUrl(url);
    }
    @Override public String[] getAvailableContentFilter() {
        return new String[] { ChannelTabs.VIDEOS, ChannelTabs.CHANNELS, ChannelTabs.PLAYLISTS };
    }
}
