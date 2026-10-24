// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;

import java.io.IOException;

public final class SoundcloudStreamLinkHandlerFactory extends LinkHandlerFactory {
    private static final SoundcloudStreamLinkHandlerFactory INSTANCE =
            new SoundcloudStreamLinkHandlerFactory();
    private SoundcloudStreamLinkHandlerFactory() { }

    public static SoundcloudStreamLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getUrl(final String id) throws ParsingException {
        try {
            return SoundcloudParsingHelper.resolveUrlWithEmbedPlayer(
                    "https://api.soundcloud.com/tracks/" + id);
        } catch (final IOException | ReCaptchaException e) {
            throw new ParsingException("Unable to create SoundCloud track URL", e);
        }
    }

    @Override public String getId(final String url) throws ParsingException {
        final String apiId = SoundcloudTrackUrlParser.INSTANCE.apiTrackId(url);
        if (apiId != null) return apiId;
        if (!SoundcloudTrackUrlParser.INSTANCE.acceptsPage(url)) {
            throw new ParsingException("Invalid SoundCloud track URL");
        }
        try {
            return SoundcloudParsingHelper.resolveIdWithWidgetApi(url);
        } catch (final IOException e) {
            throw new ParsingException("Unable to resolve SoundCloud track", e);
        }
    }

    @Override public boolean onAcceptUrl(final String url) {
        return SoundcloudTrackUrlParser.INSTANCE.acceptsPage(url)
                || SoundcloudTrackUrlParser.INSTANCE.apiTrackId(url) != null;
    }
}
