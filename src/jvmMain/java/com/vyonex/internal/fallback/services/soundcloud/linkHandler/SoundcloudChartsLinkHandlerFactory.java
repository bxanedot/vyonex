// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;
import java.util.regex.Pattern;

public final class SoundcloudChartsLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final SoundcloudChartsLinkHandlerFactory INSTANCE =
            new SoundcloudChartsLinkHandlerFactory();
    private static final Pattern URL = Pattern.compile(
            "^https?://(?:www\\.|m\\.)?soundcloud\\.com/charts(?:/top|/new)?/?(?:[?#].*)?$",
            Pattern.CASE_INSENSITIVE);

    private SoundcloudChartsLinkHandlerFactory() { }

    public static SoundcloudChartsLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override public String getId(final String url) throws ParsingException {
        if (!onAcceptUrl(url)) throw new ParsingException("Invalid SoundCloud charts URL");
        return "New & hot";
    }

    @Override public String getUrl(final String id, final List<String> contentFilter,
                                   final String sortFilter) {
        return "https://soundcloud.com/charts/new";
    }

    @Override public boolean onAcceptUrl(final String url) { return URL.matcher(url).matches(); }
}
