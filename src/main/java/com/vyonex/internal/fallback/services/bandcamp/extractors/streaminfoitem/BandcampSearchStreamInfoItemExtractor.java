// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;

public final class BandcampSearchStreamInfoItemExtractor extends BandcampStreamInfoItemExtractor {
    private final Element result;

    public BandcampSearchStreamInfoItemExtractor(final Element result, final String uploaderUrl) {
        super(uploaderUrl);
        this.result = result;
    }

    @Override
    public String getUploaderName() {
        final String subtitle = text(".subhead");
        final int separator = subtitle.toLowerCase(Locale.ROOT).lastIndexOf(" by ");
        return separator < 0 ? subtitle : subtitle.substring(separator + 4).trim();
    }

    @Override
    public String getName() throws ParsingException { return requiredText(".heading"); }

    @Override
    public String getUrl() throws ParsingException { return requiredText(".itemurl"); }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromSearchResult(result);
    }

    @Override
    public long getDuration() { return -1; }

    private String requiredText(final String selector) throws ParsingException {
        final String value = text(selector);
        if (value.isBlank()) throw new ParsingException("Bandcamp search result is missing " + selector);
        return value;
    }

    private String text(final String selector) {
        final Element value = result.selectFirst(".result-info " + selector);
        return value == null ? "" : value.text();
    }
}
