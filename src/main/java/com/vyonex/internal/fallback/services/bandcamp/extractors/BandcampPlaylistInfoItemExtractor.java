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
    private final String uploaderName;
    private final long streamCount;
    private final String name;
    private final String url;
    private final String imageUrl;

    public BandcampPlaylistInfoItemExtractor(@Nonnull final Element result) {
        final String subtitle = text(result, ".subhead");
        final int separator = subtitle.toLowerCase(Locale.ROOT).lastIndexOf(" by ");
        uploaderName = separator < 0 ? subtitle : subtitle.substring(separator + 4).trim();
        final Matcher count = LEADING_COUNT.matcher(text(result, ".length"));
        long parsedCount = -1;
        if (count.find()) {
            try {
                parsedCount = Long.parseLong(count.group(1));
            } catch (final NumberFormatException ignored) { }
        }
        streamCount = parsedCount;
        name = text(result, ".heading");
        url = text(result, ".itemurl");
        final Element image = result.selectFirst(".art img[src]");
        imageUrl = image == null ? "" : image.attr("src");
    }

    @Override public String getUploaderName() { return uploaderName; }

    @Override
    public String getUploaderUrl() { return null; }

    @Override
    public boolean isUploaderVerified() { return false; }

    @Override
    public long getStreamCount() { return streamCount; }

    @Override public String getName() { return name; }

    @Override public String getUrl() { return url; }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return BandcampExtractorHelper.getImagesFromImageUrl(imageUrl);
    }

    private static String text(final Element result, final String selector) {
        final Element value = result.selectFirst(".result-info " + selector);
        return value == null ? "" : value.text();
    }
}
