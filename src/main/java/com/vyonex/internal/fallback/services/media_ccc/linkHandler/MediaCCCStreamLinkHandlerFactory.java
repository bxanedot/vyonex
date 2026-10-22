// bxane
package com.vyonex.internal.fallback.services.media_ccc.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper;
import com.vyonex.internal.fallback.utils.Parser;

public final class MediaCCCStreamLinkHandlerFactory extends LinkHandlerFactory {

    private static final MediaCCCStreamLinkHandlerFactory INSTANCE =
            new MediaCCCStreamLinkHandlerFactory();

    public static final String VIDEO_API_ENDPOINT = "https://api.media.ccc.de/public/events/";
    private static final String VIDEO_PATH = "https://media.ccc.de/v/";
    private static final String RECORDING_ID_PATTERN
            = "(?:(?:(?:api\\.)?media\\.ccc\\.de/public/events/)"
            + "|(?:media\\.ccc\\.de/v/))([^/?&#]*)";
    private static final String LIVE_STREAM_PATH = "https://streaming.media.ccc.de/";
    private static final String LIVE_STREAM_ID_PATTERN
            = "streaming\\.media\\.ccc\\.de\\/(\\w+\\/\\w+)";

    private MediaCCCStreamLinkHandlerFactory() {
    }

    public static MediaCCCStreamLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        String streamId = null;
        try {
            streamId = Parser.matchGroup1(LIVE_STREAM_ID_PATTERN, url);
        } catch (final Parser.RegexException ignored) {
        }

        if (streamId == null) {
            return Parser.matchGroup1(RECORDING_ID_PATTERN, url);
        }
        return streamId;
    }

    @Override
    public String getUrl(final String id) throws ParsingException, UnsupportedOperationException {
        if (MediaCCCParsingHelper.isLiveStreamId(id)) {
            return LIVE_STREAM_PATH + id;
        }
        return VIDEO_PATH + id;
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        try {
            return getId(url) != null;
        } catch (final ParsingException e) {
            return false;
        }
    }
}
