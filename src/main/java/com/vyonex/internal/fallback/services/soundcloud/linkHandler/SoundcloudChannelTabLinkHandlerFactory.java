// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler;

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.UnsupportedTabException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;
import java.util.Map;

public final class SoundcloudChannelTabLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final SoundcloudChannelTabLinkHandlerFactory INSTANCE =
            new SoundcloudChannelTabLinkHandlerFactory();
    private static final Map<String, String> SUFFIXES = Map.of(
            ChannelTabs.TRACKS, "/tracks",
            ChannelTabs.PLAYLISTS, "/sets",
            ChannelTabs.ALBUMS, "/albums",
            ChannelTabs.LIKES, "/likes");

    private SoundcloudChannelTabLinkHandlerFactory() { }

    public static SoundcloudChannelTabLinkHandlerFactory getInstance() { return INSTANCE; }

    public static String getUrlSuffix(final String tab) {
        final String suffix = SUFFIXES.get(tab);
        if (suffix == null) throw new UnsupportedTabException(tab);
        return suffix;
    }

    @Override public String getId(final String url) throws ParsingException {
        return SoundcloudChannelLinkHandlerFactory.getInstance().getId(url);
    }

    @Override public String getUrl(final String id, final List<String> contentFilter,
                                   final String sortFilter) throws ParsingException {
        if (contentFilter.isEmpty()) throw new ParsingException("SoundCloud tab is required");
        return SoundcloudChannelLinkHandlerFactory.getInstance().getUrl(id)
                + getUrlSuffix(contentFilter.get(0));
    }

    @Override public boolean onAcceptUrl(final String url) {
        return SoundcloudChannelLinkHandlerFactory.getInstance().onAcceptUrl(url);
    }

    @Override public String[] getAvailableContentFilter() {
        return new String[]{ChannelTabs.TRACKS, ChannelTabs.PLAYLISTS,
                ChannelTabs.ALBUMS, ChannelTabs.LIKES};
    }
}
