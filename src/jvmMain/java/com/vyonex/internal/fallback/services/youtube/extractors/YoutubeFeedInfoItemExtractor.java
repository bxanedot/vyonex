// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.stream.StreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class YoutubeFeedInfoItemExtractor implements StreamInfoItemExtractor {
    private final Element entry;

    public YoutubeFeedInfoItemExtractor(final Element entry) { this.entry = entry; }

    @Override public StreamType getStreamType() { return StreamType.VIDEO_STREAM; }
    @Override public boolean isAd() { return false; }
    @Override public long getDuration() { return -1; }
    @Override public long getViewCount() throws ParsingException {
        final Element stats = entry.getElementsByTag("media:statistics").first();
        if (stats == null) throw new ParsingException("Feed entry has no view count");
        try {
            return Long.parseLong(stats.attr("views"));
        } catch (final NumberFormatException error) {
            throw new ParsingException("Feed entry has an invalid view count", error);
        }
    }
    @Override public String getUploaderName() { return text("author > name"); }
    @Override public String getUploaderUrl() { return text("author > uri"); }
    @Override public boolean isUploaderVerified() { return false; }
    @Nullable @Override public String getTextualUploadDate() { return text("published"); }
    @Nullable @Override public DateWrapper getUploadDate() throws ParsingException {
        return DateWrapper.fromOffsetDateTime(getTextualUploadDate());
    }
    @Override public String getName() throws ParsingException { return requiredText("title"); }
    @Override public String getUrl() throws ParsingException {
        final Element link = entry.selectFirst("link");
        if (link == null || link.attr("href").isBlank()) {
            throw new ParsingException("Feed entry has no video link");
        }
        return link.attr("href");
    }

    @Nonnull @Override public List<Image> getThumbnails() {
        final Element thumbnail = entry.getElementsByTag("media:thumbnail").first();
        if (thumbnail == null || thumbnail.attr("url").isEmpty()) return List.of();
        final String source = thumbnail.attr("url");
        final String url = source.replace("hqdefault", "mqdefault");
        final int height = url.equals(source) ? integer(thumbnail.attr("height"),
                Image.HEIGHT_UNKNOWN) : 320;
        final int width = url.equals(source) ? integer(thumbnail.attr("width"),
                Image.WIDTH_UNKNOWN) : 180;
        return List.of(new Image(url, height, width, Image.ResolutionLevel.fromHeight(height)));
    }

    private String text(final String selector) {
        final Element element = entry.selectFirst(selector);
        return element == null ? "" : element.text();
    }

    private String requiredText(final String selector) throws ParsingException {
        final String value = text(selector);
        if (value.isEmpty()) throw new ParsingException("Feed entry is missing " + selector);
        return value;
    }

    private static int integer(final String value, final int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException ignored) {
            return fallback;
        }
    }
}
