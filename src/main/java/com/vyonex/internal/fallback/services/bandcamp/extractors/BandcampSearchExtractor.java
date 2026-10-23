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
import com.vyonex.internal.fallback.exceptions.ParsingException;
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
import java.util.Objects;

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
        final MultiInfoItemsCollector results = new MultiInfoItemsCollector(getServiceId());

        for (final Element result : document.select(".searchresult")) {
            final Element type = result.selectFirst(".result-info .itemtype");
            if (type == null) continue;

            switch (type.text().trim().toUpperCase(Locale.ROOT)) {
                case "ARTIST" -> results.commit(new BandcampChannelInfoItemExtractor(result));
                case "ALBUM" -> results.commit(new BandcampPlaylistInfoItemExtractor(result));
                case "TRACK" -> results.commit(new BandcampSearchStreamInfoItemExtractor(result, null));
                default -> { }
            }
        }

        return new InfoItemsPage<>(results, findNextPage(document, page.getUrl()));
    }

    static Page findNextPage(final Document document, final String currentUrl) {
        Element link = document.selectFirst(
                ".pagelist a.next[href], .pagelist a[rel~=next][href], .pagelist li.next a[href]");
        if (link == null) {
            final Element current = currentPage(document.selectFirst(".pagelist"));
            final Element following = current == null ? null : current.nextElementSibling();
            link = following == null ? null : following.selectFirst("a[href]");
        }
        if (link == null) return null;

        try {
            final URI base = URI.create(currentUrl);
            final URI next = resolvePage(base, URI.create(link.attr("href"))).normalize();
            if (!sameOrigin(base, next)
                    || (Objects.equals(base.getRawPath(), next.getRawPath())
                    && Objects.equals(base.getRawQuery(), next.getRawQuery()))) {
                return null;
            }
            return new Page(next.toString());
        } catch (final IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }

    private static URI resolvePage(final URI base, final URI reference) {
        if (reference.getScheme() == null && reference.getRawAuthority() == null
                && (reference.getRawPath() == null || reference.getRawPath().isEmpty())
                && reference.getRawQuery() != null) {
            final String path = base.getRawPath() == null ? "" : base.getRawPath();
            final String fragment = reference.getRawFragment() == null
                    ? "" : "#" + reference.getRawFragment();
            return URI.create(base.getScheme() + "://" + base.getRawAuthority() + path
                    + "?" + reference.getRawQuery() + fragment);
        }
        return base.resolve(reference);
    }

    private static Element currentPage(final Element pagination) {
        if (pagination == null) return null;
        Element current = pagination.selectFirst("li.current, li.selected, li.active");
        if (current == null) {
            final Element number = pagination.selectFirst("li span");
            if (number != null) current = number.closest("li");
        }
        return current;
    }

    private static boolean sameOrigin(final URI first, final URI second) {
        final String scheme = first.getScheme();
        if (!isHttp(scheme) || !isHttp(second.getScheme()) || first.getHost() == null
                || second.getHost() == null || first.getRawUserInfo() != null
                || second.getRawUserInfo() != null) {
            return false;
        }
        return scheme.equalsIgnoreCase(second.getScheme())
                && first.getHost().equalsIgnoreCase(second.getHost())
                && port(first) == port(second);
    }

    private static boolean isHttp(final String scheme) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private static int port(final URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        return getPage(new Page(getUrl()));
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException { }
}
