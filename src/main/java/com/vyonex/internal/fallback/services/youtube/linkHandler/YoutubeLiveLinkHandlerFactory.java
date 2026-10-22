// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;

import java.util.List;
import java.util.Objects;

public final class YoutubeLiveLinkHandlerFactory extends ListLinkHandlerFactory {
    public static final String KIOSK_ID = "live";
    public static final YoutubeLiveLinkHandlerFactory INSTANCE = new YoutubeLiveLinkHandlerFactory();
    private static final String PATH = "/channel/UC4R8DWoMoI7CAwX8_LjQHig/livetab";
    private static final String PARAMS = "ss=CKEK";

    private YoutubeLiveLinkHandlerFactory() { }

    @Override public String getUrl(final String id, final List<String> filters, final String sort) {
        return "https://www.youtube.com" + PATH + "?" + PARAMS;
    }
    @Override public String getId(final String url) { return KIOSK_ID; }
    @Override public boolean onAcceptUrl(final String value) {
        final var url = YoutubeRouteSupport.parse(value);
        return YoutubeRouteSupport.hasPath(url, PATH) && Objects.equals(PARAMS, url.getQuery());
    }
}
