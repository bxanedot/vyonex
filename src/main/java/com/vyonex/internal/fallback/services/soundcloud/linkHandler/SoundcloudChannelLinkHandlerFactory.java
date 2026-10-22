// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.utils.Parser;
import com.vyonex.internal.fallback.utils.Utils;

import java.util.List;

public final class SoundcloudChannelLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final SoundcloudChannelLinkHandlerFactory INSTANCE
            = new SoundcloudChannelLinkHandlerFactory();
    private static final String URL_PATTERN = "^https?://(www\\.|m\\.)?soundcloud.com/[0-9a-z_-]+"
            + "(/((tracks|albums|sets|reposts|followers|following)/?)?)?([#?].*)?$";

    private SoundcloudChannelLinkHandlerFactory() {
    }

    public static SoundcloudChannelLinkHandlerFactory getInstance() {
        return INSTANCE;
    }


    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        Utils.checkUrl(URL_PATTERN, url);

        try {
            return SoundcloudParsingHelper.resolveIdWithWidgetApi(url);
        } catch (final Exception e) {
            throw new ParsingException(e.getMessage(), e);
        }
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter)
            throws ParsingException, UnsupportedOperationException {
        try {
            return SoundcloudParsingHelper.resolveUrlWithEmbedPlayer(
                    "https://api.soundcloud.com/users/" + id);
        } catch (final Exception e) {
            throw new ParsingException(e.getMessage(), e);
        }
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        return Parser.isMatch(URL_PATTERN, url.toLowerCase());
    }
}
