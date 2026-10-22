// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler;

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.UnsupportedTabException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import javax.annotation.Nonnull;
import java.util.List;

public final class SoundcloudChannelTabLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final SoundcloudChannelTabLinkHandlerFactory INSTANCE
            = new SoundcloudChannelTabLinkHandlerFactory();

    private SoundcloudChannelTabLinkHandlerFactory() {
    }

    public static SoundcloudChannelTabLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Nonnull
    public static String getUrlSuffix(final String tab) throws UnsupportedOperationException {
        switch (tab) {
            case ChannelTabs.TRACKS:
                return "/tracks";
            case ChannelTabs.PLAYLISTS:
                return "/sets";
            case ChannelTabs.ALBUMS:
                return "/albums";
            case ChannelTabs.LIKES:
                return "/likes";
        }
        throw new UnsupportedTabException(tab);
    }

    @Override
    public String getId(final String url) throws ParsingException {
        return SoundcloudChannelLinkHandlerFactory.getInstance().getId(url);
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter) throws ParsingException {
        return SoundcloudChannelLinkHandlerFactory.getInstance().getUrl(id)
                + getUrlSuffix(contentFilter.get(0));
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        return SoundcloudChannelLinkHandlerFactory.getInstance().onAcceptUrl(url);
    }

    @Override
    public String[] getAvailableContentFilter() {
        return new String[] {
                ChannelTabs.TRACKS,
                ChannelTabs.PLAYLISTS,
                ChannelTabs.ALBUMS,
                ChannelTabs.LIKES,
        };
    }
}
