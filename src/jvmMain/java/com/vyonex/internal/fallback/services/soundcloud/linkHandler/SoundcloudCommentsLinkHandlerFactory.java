// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler;

import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;

import java.io.IOException;
import java.util.List;

public final class SoundcloudCommentsLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final SoundcloudCommentsLinkHandlerFactory INSTANCE =
            new SoundcloudCommentsLinkHandlerFactory();

    private SoundcloudCommentsLinkHandlerFactory() { }

    public static SoundcloudCommentsLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getUrl(final String id, final List<String> contentFilter,
                                   final String sortFilter) throws ParsingException {
        try {
            return "https://api-v2.soundcloud.com/tracks/" + id + "/comments?client_id="
                    + SoundcloudParsingHelper.clientId() + "&threaded=0&filter_replies=1";
        } catch (final ExtractionException | IOException e) {
            throw new ParsingException("Unable to create SoundCloud comments URL", e);
        }
    }

    @Override public String getId(final String url) throws ParsingException {
        return SoundcloudStreamLinkHandlerFactory.getInstance().getId(url);
    }

    @Override public boolean onAcceptUrl(final String url) {
        return SoundcloudStreamLinkHandlerFactory.getInstance().onAcceptUrl(url);
    }
}
