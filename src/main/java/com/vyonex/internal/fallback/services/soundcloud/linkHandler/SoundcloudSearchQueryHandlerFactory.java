// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler;

import static com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL;

import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.utils.Utils;

import java.io.IOException;
import java.util.List;

public final class SoundcloudSearchQueryHandlerFactory extends SearchQueryHandlerFactory {

    private static final SoundcloudSearchQueryHandlerFactory INSTANCE =
            new SoundcloudSearchQueryHandlerFactory();

    public static final String TRACKS = "tracks";
    public static final String USERS = "users";
    public static final String PLAYLISTS = "playlists";
    public static final String ALL = "all";

    public static final int ITEMS_PER_PAGE = 10;

    private SoundcloudSearchQueryHandlerFactory() {
    }

    public static SoundcloudSearchQueryHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter)
            throws ParsingException, UnsupportedOperationException {
        try {
            String url = SOUNDCLOUD_API_V2_URL + "search";

            if (!contentFilter.isEmpty()) {
                switch (contentFilter.get(0)) {
                    case TRACKS:
                        url += "/tracks";
                        break;
                    case USERS:
                        url += "/users";
                        break;
                    case PLAYLISTS:
                        url += "/playlists";
                        break;
                    case ALL:
                    default:
                        break;
                }
            }

            return url + "?q=" + Utils.encodeUrlUtf8(id)
                    + "&client_id=" + SoundcloudParsingHelper.clientId()
                    + "&limit=" + ITEMS_PER_PAGE + "&offset=0";
        } catch (final ReCaptchaException e) {
            throw new ParsingException("ReCaptcha required", e);
        } catch (final IOException | ExtractionException e) {
            throw new ParsingException("Could not get client id", e);
        }
    }

    @Override
    public String[] getAvailableContentFilter() {
        return new String[]{
                ALL,
                TRACKS,
                USERS,
                PLAYLISTS};
    }
}
