// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public final class SoundcloudPlaylistLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final SoundcloudPlaylistLinkHandlerFactory INSTANCE =
            new SoundcloudPlaylistLinkHandlerFactory();
    private static final Pattern URL = Pattern.compile(
            "^https?://(?:www\\.|m\\.)?soundcloud\\.com/[a-z0-9_-]+/sets/[a-z0-9_-]+/?(?:[?#].*)?$",
            Pattern.CASE_INSENSITIVE);

    private SoundcloudPlaylistLinkHandlerFactory() { }

    public static SoundcloudPlaylistLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getId(final String url) throws ParsingException {
        if (!onAcceptUrl(url)) throw new ParsingException("Invalid SoundCloud playlist URL");
        try {
            return SoundcloudParsingHelper.resolveIdWithWidgetApi(url);
        } catch (final IOException e) {
            throw new ParsingException("Unable to resolve SoundCloud playlist", e);
        }
    }

    @Override public String getUrl(final String id, final List<String> contentFilter,
                                   final String sortFilter) throws ParsingException {
        try {
            return SoundcloudParsingHelper.resolveUrlWithEmbedPlayer(
                    "https://api.soundcloud.com/playlists/" + id);
        } catch (final IOException | ReCaptchaException e) {
            throw new ParsingException("Unable to create SoundCloud playlist URL", e);
        }
    }

    @Override public boolean onAcceptUrl(final String url) { return URL.matcher(url).matches(); }
}
