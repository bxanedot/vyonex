// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.UnsupportedTabException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import javax.annotation.Nonnull;
import java.util.List;

public final class YoutubeChannelTabLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final YoutubeChannelTabLinkHandlerFactory INSTANCE =
            new YoutubeChannelTabLinkHandlerFactory();

    private YoutubeChannelTabLinkHandlerFactory() {
    }

    public static YoutubeChannelTabLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Nonnull
    public static String getUrlSuffix(@Nonnull final String tab)
            throws UnsupportedTabException {
        switch (tab) {
            case ChannelTabs.VIDEOS:
                return "/videos";
            case ChannelTabs.SHORTS:
                return "/shorts";
            case ChannelTabs.LIVESTREAMS:
                return "/streams";
            case ChannelTabs.ALBUMS:
                return "/releases";
            case ChannelTabs.PLAYLISTS:
                return "/playlists";
            default:
                throw new UnsupportedTabException(tab);
        }
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter)
            throws ParsingException, UnsupportedOperationException {
        return "https://www.youtube.com/" + id + getUrlSuffix(contentFilter.get(0));
    }

    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        return YoutubeChannelLinkHandlerFactory.getInstance().getId(url);
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        try {
            getId(url);
        } catch (final ParsingException e) {
            return false;
        }
        return true;
    }

    @Override
    public String[] getAvailableContentFilter() {
        return new String[] {
                ChannelTabs.VIDEOS,
                ChannelTabs.SHORTS,
                ChannelTabs.LIVESTREAMS,
                ChannelTabs.ALBUMS,
                ChannelTabs.PLAYLISTS
        };
    }
}
