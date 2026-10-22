// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandlerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.BASE_URL;

public final class BandcampSearchQueryHandlerFactory extends SearchQueryHandlerFactory {
    private static final BandcampSearchQueryHandlerFactory INSTANCE =
            new BandcampSearchQueryHandlerFactory();

    private BandcampSearchQueryHandlerFactory() { }

    public static BandcampSearchQueryHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getUrl(final String query, final List<String> contentFilter,
                         final String sortFilter) throws ParsingException {
        return BASE_URL + "/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&page=1";
    }
}
