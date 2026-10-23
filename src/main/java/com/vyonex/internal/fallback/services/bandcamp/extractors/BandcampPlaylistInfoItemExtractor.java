// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BandcampPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private static final Pattern LEADING_COUNT = Pattern.compile("^\\s*(\\d+)");
    private final Element result;

    public BandcampPlaylistInfoItemExtractor(@Nonnull final Element result) {
        this.result = result;
    }

    @Override
    public String getUploaderName() {
        final String subtitle = text(".subhead");
        final int separator = subtitle.toLowerCase(Locale.ROOT).lastIndexOf(" by ");
        return separator < 0 ? subtitle : subtitle.substring(separator + 4).trim();
    }

    @Override
    public String getUploaderUrl() { return null; }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Override
    public long getStreamCount() {
        final Matcher count = LEADING_COUNT.matcher(text(".length"));
        if (!count.find()) return -1;
        try {
            return Long.parseLong(count.group(1));
        } catch (final NumberFormatException ignored) {
            return -1;
        }
    }

    @Override
    public String getName() { return text(".heading"); }

    @Override
    public String getUrl() { return text(".itemurl"); }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromSearchResult(result);
    }

    private String text(final String selector) {
        final Element value = result.selectFirst(".result-info " + selector);
        return value == null ? "" : value.text();
    }
}
