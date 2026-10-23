// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.UnsupportedTabException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;
import java.util.Map;

public final class YoutubeChannelTabLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final YoutubeChannelTabLinkHandlerFactory INSTANCE =
            new YoutubeChannelTabLinkHandlerFactory();
    private static final Map<String, String> SUFFIXES = Map.of(
            ChannelTabs.VIDEOS, "/videos",
            ChannelTabs.SHORTS, "/shorts",
            ChannelTabs.LIVESTREAMS, "/streams",
            ChannelTabs.ALBUMS, "/releases",
            ChannelTabs.PLAYLISTS, "/playlists");

    private YoutubeChannelTabLinkHandlerFactory() { }

    public static YoutubeChannelTabLinkHandlerFactory getInstance() { return INSTANCE; }

    public static String getUrlSuffix(final String tab) {
        final String suffix = SUFFIXES.get(tab);
        if (suffix == null) throw new UnsupportedTabException(tab);
        return suffix;
    }

    @Override public String getUrl(final String id, final List<String> filters, final String sort)
            throws ParsingException {
        if (filters.isEmpty()) throw new ParsingException("YouTube channel tab is required");
        return "https://www.youtube.com/" + id + getUrlSuffix(filters.get(0));
    }
    @Override public String getId(final String url) throws ParsingException {
        return YoutubeChannelLinkHandlerFactory.getInstance().getId(url);
    }
    @Override public boolean onAcceptUrl(final String url) {
        try {
            getId(url);
            return true;
        } catch (final ParsingException e) {
            return false;
        }
    }
    @Override public String[] getAvailableContentFilter() {
        return new String[]{ChannelTabs.VIDEOS, ChannelTabs.SHORTS, ChannelTabs.LIVESTREAMS,
                ChannelTabs.ALBUMS, ChannelTabs.PLAYLISTS};
    }
}
