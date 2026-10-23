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
    private final String uploaderName;
    private final String name;
    private final String url;
    private final String imageUrl;

    public BandcampSearchStreamInfoItemExtractor(final Element result, final String uploaderUrl) {
        super(uploaderUrl);
        final String subtitle = text(result, ".subhead");
        final int separator = subtitle.toLowerCase(Locale.ROOT).lastIndexOf(" by ");
        uploaderName = separator < 0 ? subtitle : subtitle.substring(separator + 4).trim();
        name = text(result, ".heading");
        url = text(result, ".itemurl");
        final Element image = result.selectFirst(".art img[src]");
        imageUrl = image == null ? "" : image.attr("src");
    }

    @Override public String getUploaderName() { return uploaderName; }

    @Override
    public String getName() throws ParsingException { return required(name, ".heading"); }

    @Override
    public String getUrl() throws ParsingException { return required(url, ".itemurl"); }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageUrl(imageUrl);
    }

    @Override
    public long getDuration() { return -1; }

    private String required(final String value, final String selector) throws ParsingException {
        if (value.isBlank()) throw new ParsingException("Bandcamp search result is missing " + selector);
        return value;
    }

    private static String text(final Element result, final String selector) {
        final Element value = result.selectFirst(".result-info " + selector);
        return value == null ? "" : value.text();
    }
}
