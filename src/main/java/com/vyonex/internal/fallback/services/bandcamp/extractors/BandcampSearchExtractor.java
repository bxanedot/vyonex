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
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BandcampSearchExtractor extends SearchExtractor {

    public BandcampSearchExtractor(final StreamingService service,
                                   final SearchQueryHandler linkHandler) {
        super(service, linkHandler);
    }

    @Nonnull
    @Override
    public String getSearchSuggestion() {
        return "";
    }

    @Override
    public boolean isCorrectedSearch() {
        return false;
    }

    @Nonnull
    @Override
    public List<MetaInfo> getMetaInfo() throws ParsingException {
        return Collections.emptyList();
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        final String html = getDownloader().get(page.getUrl(), Map.of("Cookie", List.of("identity")))
                .responseBody();
        final Document document = Jsoup.parse(html, page.getUrl());
        final MultiInfoItemsCollector items = new MultiInfoItemsCollector(getServiceId());

        for (final Element result : document.select(".searchresult")) {
            final Element resultInfo = result.selectFirst(".result-info");
            if (resultInfo == null) {
                continue;
            }

            final Element itemType = resultInfo.selectFirst(".itemtype");
            if (itemType == null) {
                continue;
            }

            switch (itemType.text().trim().toUpperCase(Locale.ROOT)) {
                case "ARTIST":
                    items.commit(new BandcampChannelInfoItemExtractor(result));
                    break;
                case "ALBUM":
                    items.commit(new BandcampPlaylistInfoItemExtractor(result));
                    break;
                case "TRACK":
                    items.commit(new BandcampSearchStreamInfoItemExtractor(result, null));
                    break;
                default:
                    break;
            }
        }

        return new InfoItemsPage<>(items, findNextPage(document, page.getUrl()));
    }

    static Page findNextPage(final Document document, final String currentUrl) {
        final Element pagination = document.selectFirst(".pagelist");
        if (pagination == null) {
            return null;
        }

        Element nextLink = pagination.selectFirst("a.next[href], a[rel~=next][href], li.next a[href]");
        if (nextLink == null) {
            Element current = pagination.selectFirst("li.current, li.selected, li.active");
            if (current == null) {
                final Element currentNumber = pagination.selectFirst("li span");
                if (currentNumber != null) {
                    current = currentNumber.closest("li");
                }
            }
            if (current != null && current.nextElementSibling() != null) {
                nextLink = current.nextElementSibling().selectFirst("a[href]");
            }
        }
        if (nextLink == null) {
            return null;
        }

        try {
            final URI base = new URI(currentUrl);
            final URI next = resolvePageUri(base, nextLink.attr("href"));
            if (!isHttp(base) || !isHttp(next)
                    || !base.getScheme().equalsIgnoreCase(next.getScheme())
                    || base.getHost() == null || !base.getHost().equalsIgnoreCase(next.getHost())
                    || effectivePort(base) != effectivePort(next)
                    || next.getRawUserInfo() != null || next.equals(base)) {
                return null;
            }
            return new Page(next.toString());
        } catch (final URISyntaxException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private static URI resolvePageUri(final URI base, final String href) throws URISyntaxException {
        final URI reference = new URI(href);
        if (reference.getScheme() == null && reference.getRawAuthority() == null
                && (reference.getRawPath() == null || reference.getRawPath().isEmpty())
                && reference.getRawQuery() != null) {
            final String path = base.getRawPath() == null ? "" : base.getRawPath();
            final String fragment = reference.getRawFragment() == null
                    ? "" : "#" + reference.getRawFragment();
            return new URI(base.getScheme() + "://" + base.getRawAuthority() + path
                    + "?" + reference.getRawQuery() + fragment).normalize();
        }
        return base.resolve(reference).normalize();
    }

    private static boolean isHttp(final URI uri) {
        return "http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme());
    }

    private static int effectivePort(final URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        return getPage(new Page(getUrl()));
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
    }
}
