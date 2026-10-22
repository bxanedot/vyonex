// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler;

import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.UnsupportedTabException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import javax.annotation.Nonnull;
import java.util.List;

public final class PeertubeChannelTabLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final PeertubeChannelTabLinkHandlerFactory INSTANCE
            = new PeertubeChannelTabLinkHandlerFactory();

    private PeertubeChannelTabLinkHandlerFactory() {
    }

    public static PeertubeChannelTabLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Nonnull
    public static String getUrlSuffix(@Nonnull final String tab)
            throws UnsupportedTabException {
        switch (tab) {
            case ChannelTabs.VIDEOS:
                return "/videos";
            case ChannelTabs.CHANNELS: // only available on accounts
                return "/video-channels";
            case ChannelTabs.PLAYLISTS: // only available on channels
                return "/video-playlists";
        }
        throw new UnsupportedTabException(tab);
    }

    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        return PeertubeChannelLinkHandlerFactory.getInstance().getId(url);
    }

    @Override
    public String getUrl(final String id, final List<String> contentFilter, final String sortFilter)
            throws ParsingException, UnsupportedOperationException {
        return PeertubeChannelLinkHandlerFactory.getInstance().getUrl(id)
                + getUrlSuffix(contentFilter.get(0));
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter,
                         final String baseUrl)
            throws ParsingException, UnsupportedOperationException {
        return PeertubeChannelLinkHandlerFactory.getInstance().getUrl(id, null, null, baseUrl)
                + getUrlSuffix(contentFilter.get(0));
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        return PeertubeChannelLinkHandlerFactory.getInstance().onAcceptUrl(url);
    }

    @Override
    public String[] getAvailableContentFilter() {
        return new String[] {
                ChannelTabs.VIDEOS,
                ChannelTabs.CHANNELS,
                ChannelTabs.PLAYLISTS,
        };
    }
}
