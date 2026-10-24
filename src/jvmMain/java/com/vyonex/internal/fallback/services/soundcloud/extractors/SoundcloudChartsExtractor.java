// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.kiosk.KioskExtractor;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.fallback.stream.StreamInfoItemsCollector;

import java.io.IOException;

public final class SoundcloudChartsExtractor extends KioskExtractor<StreamInfoItem> {
    private String nextUrl;
    private StreamInfoItemsCollector firstPage;

    public SoundcloudChartsExtractor(final StreamingService service, final ListLinkHandler handler,
                                     final String kioskId) {
        super(service, handler, kioskId);
    }

    @Override public void onFetchPage(final Downloader downloader)
            throws ExtractionException, IOException {
        if (firstPage != null) return;
        firstPage = new StreamInfoItemsCollector(getServiceId());
        final String base = SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL + "charts"
                + "?genre=soundcloud%3Agenres%3Aall-music&client_id="
                + SoundcloudParsingHelper.clientId() + "&kind=trending";
        final ContentCountry country = getService().getContentCountry();
        final String regional = getService().getSupportedCountries().contains(country)
                ? base + "&region=soundcloud%3Aregions%3A" + country.getCountryCode() : base;
        try {
            nextUrl = SoundcloudParsingHelper.getStreamsFromApi(firstPage, regional, true);
        } catch (final IOException e) {
            if (regional.equals(base)) throw e;
            nextUrl = SoundcloudParsingHelper.getStreamsFromApi(firstPage, base, true);
        }
    }

    @Override public String getName() { return getId(); }

    @Override public InfoItemsPage<StreamInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        if (firstPage == null) throw new IllegalStateException("Charts have not been fetched");
        return new InfoItemsPage<>(firstPage, nextUrl == null || nextUrl.isBlank()
                ? null : new Page(nextUrl));
    }

    @Override public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
            throw new IllegalArgumentException("Page URL is required");
        }
        final StreamInfoItemsCollector items = new StreamInfoItemsCollector(getServiceId());
        final String next = SoundcloudParsingHelper.getStreamsFromApi(items, page.getUrl(), true);
        return new InfoItemsPage<>(items, next.isBlank() ? null : new Page(next));
    }
}
