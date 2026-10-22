// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.util.List;

public class BandcampChannelInfoItemExtractor implements ChannelInfoItemExtractor {
    private final Element searchResult;
    private final Element resultInfo;

    public BandcampChannelInfoItemExtractor(final Element searchResult) {
        this.searchResult = searchResult;
        resultInfo = searchResult.selectFirst(".result-info");
    }

    @Override
    public String getName() throws ParsingException { return text(".heading"); }

    @Override
    public String getUrl() throws ParsingException {
        final Element link = resultInfo == null ? null : resultInfo.selectFirst(".itemurl");
        if (link == null) throw new ParsingException("Bandcamp search result has no artist URL");
        return link.text();
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromSearchResult(searchResult);
    }

    @Override
    public String getDescription() { return text(".subhead"); }

    @Override
    public long getSubscriberCount() { return -1; }

    @Override
    public long getStreamCount() { return -1; }

    @Override
    public boolean isVerified() { return false; }

    private String text(final String selector) {
        final Element element = resultInfo == null ? null : resultInfo.selectFirst(selector);
        return element == null ? "" : element.text();
    }
}
