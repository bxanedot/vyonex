// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public final class SoundcloudChannelLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final SoundcloudChannelLinkHandlerFactory INSTANCE =
            new SoundcloudChannelLinkHandlerFactory();
    private static final Pattern URL = Pattern.compile(
            "^https?://(?:www\\.|m\\.)?soundcloud\\.com/[a-z0-9_-]+"
                    + "(?:/(?:tracks|albums|sets|reposts|followers|following|likes))?/?(?:[?#].*)?$",
            Pattern.CASE_INSENSITIVE);

    private SoundcloudChannelLinkHandlerFactory() { }

    public static SoundcloudChannelLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getId(final String url) throws ParsingException {
        requireUrl(url);
        try {
            return SoundcloudParsingHelper.resolveIdWithWidgetApi(url);
        } catch (final IOException e) {
            throw new ParsingException("Unable to resolve SoundCloud channel", e);
        }
    }

    @Override public String getUrl(final String id, final List<String> contentFilter,
                                   final String sortFilter) throws ParsingException {
        try {
            return SoundcloudParsingHelper.resolveUrlWithEmbedPlayer(
                    "https://api.soundcloud.com/users/" + id);
        } catch (final IOException | com.vyonex.internal.fallback.exceptions.ReCaptchaException e) {
            throw new ParsingException("Unable to create SoundCloud channel URL", e);
        }
    }

    @Override public boolean onAcceptUrl(final String url) { return URL.matcher(url).matches(); }

    private static void requireUrl(final String url) throws ParsingException {
        if (!URL.matcher(url).matches()) throw new ParsingException("Invalid SoundCloud channel URL");
    }
}
