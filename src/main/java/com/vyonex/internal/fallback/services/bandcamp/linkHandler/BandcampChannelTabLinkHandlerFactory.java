// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler;

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.UnsupportedTabException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import javax.annotation.Nonnull;
import java.util.List;

public final class BandcampChannelTabLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final BandcampChannelTabLinkHandlerFactory INSTANCE =
            new BandcampChannelTabLinkHandlerFactory();

    private BandcampChannelTabLinkHandlerFactory() { }

    public static BandcampChannelTabLinkHandlerFactory getInstance() { return INSTANCE; }

    @Nonnull
    public static String getUrlSuffix(@Nonnull final String tab) throws UnsupportedTabException {
        return switch (tab) {
            case ChannelTabs.TRACKS -> "/track";
            case ChannelTabs.ALBUMS -> "/album";
            default -> throw new UnsupportedTabException(tab);
        };
    }

    @Override
    public String getId(final String url) throws ParsingException {
        return BandcampChannelLinkHandlerFactory.getInstance().getId(url);
    }

    @Override
    public String getUrl(final String id, final List<String> contentFilter,
                         final String sortFilter) throws ParsingException {
        return BandcampChannelLinkHandlerFactory.getInstance().getUrl(id)
                + getUrlSuffix(contentFilter.get(0));
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        return BandcampChannelLinkHandlerFactory.getInstance().onAcceptUrl(url);
    }

    @Override
    public String[] getAvailableContentFilter() {
        return new String[] { ChannelTabs.TRACKS, ChannelTabs.ALBUMS };
    }
}
