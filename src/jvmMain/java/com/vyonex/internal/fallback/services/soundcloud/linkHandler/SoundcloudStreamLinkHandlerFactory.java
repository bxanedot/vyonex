// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.LinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SoundcloudStreamLinkHandlerFactory extends LinkHandlerFactory {
    private static final SoundcloudStreamLinkHandlerFactory INSTANCE =
            new SoundcloudStreamLinkHandlerFactory();
    private static final Pattern PAGE_URL = Pattern.compile(
            "^https?://(?:(?:www|m)\\.)?soundcloud\\.com/[a-z0-9_-]+/"
                    + "(?!tracks/?$|albums/?$|sets/?$|reposts/?$|followers/?$|following/?$)"
                    + "[a-z0-9_-]+/?(?:[?#].*)?$|^https?://on\\.soundcloud\\.com/[a-z0-9]+/?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern API_URL = Pattern.compile(
            "^https?://api-v2\\.soundcloud\\.com/(?:tracks|albums|sets)/([^/?#]+)(?:[/?#].*)?$",
            Pattern.CASE_INSENSITIVE);

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
        final Matcher api = API_URL.matcher(url);
        if (api.matches()) return api.group(1);
        if (!PAGE_URL.matcher(url).matches()) throw new ParsingException("Invalid SoundCloud track URL");
        try {
            return SoundcloudParsingHelper.resolveIdWithWidgetApi(url);
        } catch (final IOException e) {
            throw new ParsingException("Unable to resolve SoundCloud track", e);
        }
    }

    @Override public boolean onAcceptUrl(final String url) {
        return PAGE_URL.matcher(url).matches() || API_URL.matcher(url).matches();
    }
}
