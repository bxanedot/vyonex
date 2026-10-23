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

public final class BandcampChannelInfoItemExtractor implements ChannelInfoItemExtractor {
    private final String name;
    private final String url;
    private final String imageUrl;
    private final String description;

    public BandcampChannelInfoItemExtractor(final Element result) {
        name = text(result, ".heading");
        url = text(result, ".itemurl");
        description = text(result, ".subhead");
        final Element image = result.selectFirst(".art img[src]");
        imageUrl = image == null ? "" : image.attr("src");
    }

    @Override public String getName() { return name; }

    @Override
    public String getUrl() throws ParsingException {
        if (url.isBlank()) throw new ParsingException("Bandcamp artist result has no URL");
        return url;
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageUrl(imageUrl);
    }

    @Override public String getDescription() { return description; }

    @Override
    public long getSubscriberCount() { return -1; }

    @Override
    public long getStreamCount() { return -1; }

    @Override
    public boolean isVerified() { return false; }

    private static String text(final Element result, final String selector) {
        final Element value = result.selectFirst(".result-info " + selector);
        return value == null ? "" : value.text();
    }
}
