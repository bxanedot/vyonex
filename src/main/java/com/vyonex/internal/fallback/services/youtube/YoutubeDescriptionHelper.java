// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import org.jsoup.nodes.Entities;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YoutubeDescriptionHelper {
    private static final Pattern LINK_CHIP = Pattern.compile("(?s)^\\s*[/•]\\s+(.+?)\\s*$");

    private YoutubeDescriptionHelper() { }

    public static String attributedDescriptionToHtml(final JsonObject description) {
        if (description == null || description.isEmpty()) return null;
        final String content = description.getString("content");
        if (content == null) return null;

        final List<Span> spans = new ArrayList<>();
        addLinks(description.getArray("commandRuns"), content, spans);
        addStyles(description.getArray("styleRuns"), content.length(), spans);
        return render(content.replace('\u00a0', ' '), spans);
    }

    private static void addLinks(final JsonArray runs, final String content,
                                 final List<Span> spans) {
        for (final Object value : runs) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject run = (JsonObject) value;
            final int start = run.getInt("startIndex", -1);
            final int end = boundedEnd(start, run.getInt("length", 0), content.length());
            if (end < 0) continue;

            final String url;
            try {
                url = YoutubeParsingHelper.getUrlFromNavigationEndpoint(
                        run.getObject("onTap").getObject("innertubeCommand"));
            } catch (final RuntimeException ignored) {
                continue;
            }
            final String href = safeHttpUrl(url);
            if (href == null) continue;

            boolean internal = false;
            try {
                final URL parsed = new URL(href);
                internal = YoutubeParsingHelper.isYoutubeURL(parsed)
                        || YoutubeParsingHelper.isYoutubeServiceURL(parsed);
            } catch (final MalformedURLException ignored) {
                // safeHttpUrl has already validated the URL.
            }

            String label = run.getObject("onTapOptions").getObject("accessibilityInfo")
                    .getString("accessibilityLabel", "");
            final int channelLabel = label.indexOf(" Channel Link");
            if (channelLabel >= 0) {
                label = label.substring(0, channelLabel) + label.substring(channelLabel + 13);
            }
            final String rawText = content.substring(start, end);
            final String linkText = !internal && !label.isEmpty()
                    && !label.startsWith("YouTube: ") ? label : cleanChip(rawText);
            spans.add(new Span(start, end, spans.size(), "<a href=\""
                    + Entities.escape(href) + "\">", "</a>", linkText));
        }
    }

    private static void addStyles(final JsonArray runs, final int contentLength,
                                  final List<Span> spans) {
        for (final Object value : runs) {
            if (!(value instanceof JsonObject)) continue;
            final JsonObject run = (JsonObject) value;
            final int start = run.getInt("startIndex", -1);
            final int end = boundedEnd(start, run.getInt("length", 0), contentLength);
            if (end < 0) continue;
            if (run.has("strikethrough")) spans.add(new Span(start, end, spans.size(), "<s>",
                    "</s>", null));
            if (run.getBoolean("italic", false)) spans.add(new Span(start, end, spans.size(),
                    "<i>", "</i>", null));
            if (run.has("weightLabel")
                    && !"FONT_WEIGHT_NORMAL".equals(run.getString("weightLabel"))) {
                spans.add(new Span(start, end, spans.size(), "<b>", "</b>", null));
            }
        }
    }

    private static int boundedEnd(final int start, final int length, final int contentLength) {
        if (start < 0 || length < 1 || start >= contentLength) return -1;
        return (int) Math.min(contentLength, (long) start + length);
    }

    private static String safeHttpUrl(final String value) {
        if (value == null) return null;
        try {
            final URI uri = new URI(value);
            final String scheme = uri.getScheme();
            if (uri.getHost() == null || scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return null;
            }
            return uri.toASCIIString();
        } catch (final URISyntaxException e) {
            return null;
        }
    }

    private static String cleanChip(final String text) {
        final Matcher matcher = LINK_CHIP.matcher(text);
        return matcher.matches() ? matcher.group(1) : text;
    }

    private static String render(final String content, final List<Span> spans) {
        final TreeSet<Integer> boundaries = new TreeSet<>();
        boundaries.add(0);
        boundaries.add(content.length());
        for (final Span span : spans) {
            boundaries.add(span.start);
            boundaries.add(span.end);
        }
        final List<Integer> points = new ArrayList<>(boundaries);
        final List<Span> open = new ArrayList<>();
        final StringBuilder html = new StringBuilder();
        final Comparator<Span> order = Comparator.comparingInt((Span span) -> span.start)
                .thenComparing(Comparator.comparingInt((Span span) -> span.end).reversed())
                .thenComparingInt(span -> span.order);

        for (int i = 0; i + 1 < points.size(); i++) {
            final int start = points.get(i);
            final int end = points.get(i + 1);
            final List<Span> active = new ArrayList<>();
            for (final Span span : spans) {
                if (span.start <= start && span.end >= end) active.add(span);
            }
            active.sort(order);

            int common = 0;
            while (common < open.size() && common < active.size()
                    && open.get(common) == active.get(common)) common++;
            for (int j = open.size() - 1; j >= common; j--) html.append(open.get(j).close);
            for (int j = common; j < active.size(); j++) html.append(active.get(j).open);
            open.clear();
            open.addAll(active);

            Span replacement = null;
            for (final Span span : active) {
                if (span.replacement != null) {
                    replacement = span;
                    break;
                }
            }
            if (replacement == null) {
                html.append(escapeText(content.substring(start, end)));
            } else if (replacement.start == start) {
                html.append(escapeText(replacement.replacement));
            }
        }
        for (int i = open.size() - 1; i >= 0; i--) html.append(open.get(i).close);
        return html.toString();
    }

    private static String escapeText(final String text) {
        return Entities.escape(text).replace("\n", "<br>").replace("  ", " &nbsp;");
    }

    private static final class Span {
        private final int start;
        private final int end;
        private final int order;
        private final String open;
        private final String close;
        private final String replacement;

        private Span(final int start, final int end, final int order, final String open,
                     final String close, final String replacement) {
            this.start = start;
            this.end = end;
            this.order = order;
            this.open = open;
            this.close = close;
            this.replacement = replacement;
        }
    }
}
