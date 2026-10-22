// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler;

import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.io.IOException;
import java.util.List;

import static com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper.clientId;

public final class SoundcloudCommentsLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final SoundcloudCommentsLinkHandlerFactory INSTANCE =
            new SoundcloudCommentsLinkHandlerFactory();

    private SoundcloudCommentsLinkHandlerFactory() {
    }

    public static SoundcloudCommentsLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter)
            throws ParsingException, UnsupportedOperationException {
        try {
            return "https://api-v2.soundcloud.com/tracks/" + id + "/comments" + "?client_id="
                    + clientId() + "&threaded=0" + "&filter_replies=1";
            // Anything but 1 = sort by new
            // + "&limit=NUMBER_OF_ITEMS_PER_REQUEST". We let the API control (default = 10)
            // + "&offset=OFFSET". We let the API control (default = 0, then we use nextPageUrl)
        } catch (final ExtractionException | IOException e) {
            throw new ParsingException("Could not get comments");
        }
    }

    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        // Delegation to avoid duplicate code, as we need the same id
        return SoundcloudStreamLinkHandlerFactory.getInstance().getId(url);
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        try {
            getId(url);
            return true;
        } catch (final ParsingException e) {
            return false;
        }
    }
}
