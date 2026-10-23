// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.linkHandler;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.List;

public final class BandcampChannelLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final BandcampChannelLinkHandlerFactory INSTANCE =
            new BandcampChannelLinkHandlerFactory();

    private BandcampChannelLinkHandlerFactory() { }

    public static BandcampChannelLinkHandlerFactory getInstance() { return INSTANCE; }

    @Override
    public String getId(final String url) throws ParsingException {
        try {
            final String html = VyonEx.getDownloader().get(BandcampUrl.https(url)).responseBody();
            final String embeddedData = Jsoup.parse(html).getElementsByAttribute("data-band")
                    .attr("data-band");
            final JsonObject band = JsonParser.object().from(embeddedData);
            return String.valueOf(band.getLong("id"));
        } catch (IOException | ReCaptchaException | JsonParserException e) {
            throw new ParsingException("Could not read artist data", e);
        }
    }

    @Override
    public String getUrl(final String id, final List<String> contentFilter,
                         final String sortFilter) throws ParsingException {
        final JsonObject details = BandcampExtractorHelper.getArtistDetails(id);
        if (details.getBoolean("error")) {
            throw new ParsingException("Artist ID has no Bandcamp profile");
        }
        return BandcampUrl.https(details.getString("bandcamp_url"));
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        return BandcampUrl.isArtistPage(url);
    }
}
