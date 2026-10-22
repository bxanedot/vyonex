// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItemExtractor;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BandcampPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private static final Pattern TRACK_COUNT = Pattern.compile("^\\s*(\\d+)");
    private final Element searchResult;
    private final Element resultInfo;

    public BandcampPlaylistInfoItemExtractor(@Nonnull final Element searchResult) {
        this.searchResult = searchResult;
        resultInfo = searchResult.selectFirst(".result-info");
    }

    @Override
    public String getUploaderName() {
        final String subtitle = text(".subhead");
        final int by = subtitle.toLowerCase(java.util.Locale.ROOT).lastIndexOf(" by ");
        return by < 0 ? subtitle : subtitle.substring(by + 4).trim();
    }

    @Override
    public String getUploaderUrl() { return null; }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Override
    public long getStreamCount() {
        final Matcher count = TRACK_COUNT.matcher(text(".length"));
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
        return BandcampExtractorHelper.getImagesFromSearchResult(searchResult);
    }

    private String text(final String selector) {
        final Element element = resultInfo == null ? null : resultInfo.selectFirst(selector);
        return element == null ? "" : element.text();
    }
}
