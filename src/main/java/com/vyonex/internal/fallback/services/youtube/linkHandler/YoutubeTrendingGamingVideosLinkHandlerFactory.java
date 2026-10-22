// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.isInvidiousURL;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.isYoutubeURL;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.utils.Utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public final class YoutubeTrendingGamingVideosLinkHandlerFactory extends ListLinkHandlerFactory {

    public static final String KIOSK_ID = "trending_gaming";

    public static final YoutubeTrendingGamingVideosLinkHandlerFactory INSTANCE =
            new YoutubeTrendingGamingVideosLinkHandlerFactory();

    private YoutubeTrendingGamingVideosLinkHandlerFactory() {
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilters,
                         final String sortFilter)
            throws ParsingException, UnsupportedOperationException {
        return "https://www.youtube.com/gaming/trending";
    }

    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        return KIOSK_ID;
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        final URL urlObj;
        try {
            urlObj = Utils.stringToURL(url);
        } catch (final MalformedURLException e) {
            return false;
        }

        return Utils.isHTTP(urlObj) && (isYoutubeURL(urlObj) || isInvidiousURL(urlObj))
                && "/gaming/trending".equals(urlObj.getPath());
    }
}
