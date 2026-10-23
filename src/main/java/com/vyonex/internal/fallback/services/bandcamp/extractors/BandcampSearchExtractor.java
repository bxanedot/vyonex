// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MetaInfo;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.linkhandler.SearchQueryHandler;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem.BandcampSearchStreamInfoItemExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BandcampSearchExtractor extends SearchExtractor {
    public BandcampSearchExtractor(final StreamingService service,
                                   final SearchQueryHandler queryHandler) {
        super(service, queryHandler);
    }

    @Nonnull
    @Override
    public String getSearchSuggestion() { return ""; }

    @Override
    public boolean isCorrectedSearch() { return false; }

    @Nonnull
    @Override
    public List<MetaInfo> getMetaInfo() { return Collections.emptyList(); }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        final String html = getDownloader().get(page.getUrl(), Map.of("Cookie", List.of("identity")))
                .responseBody();
        final Document document = Jsoup.parse(html, page.getUrl());
        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());

        for (final Element card : document.select(".searchresult")) {
            final Element label = card.selectFirst(".result-info .itemtype");
            if (label == null) continue;

            switch (label.text().trim().toUpperCase(Locale.ROOT)) {
                case "ARTIST" -> items.commit(new BandcampChannelInfoItemExtractor(card));
                case "ALBUM" -> items.commit(new BandcampPlaylistInfoItemExtractor(card));
                case "TRACK" -> items.commit(new BandcampSearchStreamInfoItemExtractor(card, null));
                default -> { }
            }
        }

        return new InfoItemsPage<>(items, findNextPage(document, page.getUrl()));
    }

    static Page findNextPage(final Document document, final String currentUrl) {
        Element next = document.selectFirst(
                ".pagelist a.next[href], .pagelist a[rel~=next][href], .pagelist li.next a[href]");
        if (next == null) {
            final Element pages = document.selectFirst(".pagelist");
            final Element selected = pages == null ? null : selectedPage(pages);
            final Element following = selected == null ? null : selected.nextElementSibling();
            next = following == null ? null : following.selectFirst("a[href]");
        }
        if (next == null) return null;

        try {
            final URI base = URI.create(currentUrl);
            final URI target = resolvePageLink(base, URI.create(next.attr("href"))).normalize();
            if (!sameWebOrigin(base, target)
                    || (java.util.Objects.equals(base.getRawPath(), target.getRawPath())
                    && java.util.Objects.equals(base.getRawQuery(), target.getRawQuery()))) {
                return null;
            }
            return new Page(target.toString());
        } catch (final IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }

    private static URI resolvePageLink(final URI base, final URI link) {
        if (link.getScheme() == null && link.getRawAuthority() == null
                && (link.getRawPath() == null || link.getRawPath().isEmpty())
                && link.getRawQuery() != null) {
            final String path = base.getRawPath() == null ? "" : base.getRawPath();
            final String fragment = link.getRawFragment() == null ? "" : "#" + link.getRawFragment();
            return URI.create(base.getScheme() + "://" + base.getRawAuthority() + path
                    + "?" + link.getRawQuery() + fragment);
        }
        return base.resolve(link);
    }

    private static Element selectedPage(final Element pagination) {
        Element selected = pagination.selectFirst("li.current, li.selected, li.active");
        if (selected == null) {
            final Element number = pagination.selectFirst("li span");
            if (number != null) selected = number.closest("li");
        }
        return selected;
    }

    private static boolean sameWebOrigin(final URI base, final URI target) {
        if (!isWebUrl(base) || !isWebUrl(target)
                || base.getHost() == null || target.getHost() == null
                || base.getRawUserInfo() != null || target.getRawUserInfo() != null) {
            return false;
        }
        return base.getScheme().equalsIgnoreCase(target.getScheme())
                && base.getHost().equalsIgnoreCase(target.getHost())
                && effectivePort(base) == effectivePort(target);
    }

    private static boolean isWebUrl(final URI uri) {
        return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
    }

    private static int effectivePort(final URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        return getPage(new Page(getUrl()));
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) { }
}
