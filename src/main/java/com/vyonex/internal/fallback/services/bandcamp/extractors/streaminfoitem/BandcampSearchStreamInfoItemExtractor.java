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

public class BandcampSearchStreamInfoItemExtractor extends BandcampStreamInfoItemExtractor {
    private final Element searchResult;
    private final Element resultInfo;

    public BandcampSearchStreamInfoItemExtractor(final Element searchResult,
                                                 final String uploaderUrl) {
        super(uploaderUrl);
        this.searchResult = searchResult;
        resultInfo = searchResult.selectFirst(".result-info");
    }

    @Override
    public String getUploaderName() {
        final Element subtitle = resultInfo == null ? null : resultInfo.selectFirst(".subhead");
        if (subtitle == null) return "";
        final String text = subtitle.text();
        final int by = text.toLowerCase(java.util.Locale.ROOT).lastIndexOf(" by ");
        return by < 0 ? text : text.substring(by + 4).trim();
    }

    @Override
    public String getName() throws ParsingException { return text(".heading"); }

    @Override
    public String getUrl() throws ParsingException { return text(".itemurl"); }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromSearchResult(searchResult);
    }

    @Override
    public long getDuration() { return -1; }

    private String text(final String selector) throws ParsingException {
        final Element element = resultInfo == null ? null : resultInfo.selectFirst(selector);
        if (element == null || element.text().isBlank()) {
            throw new ParsingException("Bandcamp search result is missing " + selector);
        }
        return element.text();
    }
}
