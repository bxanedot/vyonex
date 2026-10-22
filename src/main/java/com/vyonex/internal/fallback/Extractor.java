// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.localization.TimeAgoParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

public abstract class Extractor {
    private final StreamingService service;
    private final LinkHandler linkHandler;
    private final Downloader downloader;
    @Nullable private Localization forcedLocalization;
    @Nullable private ContentCountry forcedContentCountry;
    private boolean pageFetched;

    protected Extractor(final StreamingService service, final LinkHandler linkHandler) {
        this.service = Objects.requireNonNull(service, "service is null");
        this.linkHandler = Objects.requireNonNull(linkHandler, "LinkHandler is null");
        downloader = Objects.requireNonNull(VyonEx.getDownloader(), "downloader is null");
    }

    @Nonnull public LinkHandler getLinkHandler() { return linkHandler; }

    public void fetchPage() throws IOException, ExtractionException {
        if (pageFetched) return;
        onFetchPage(downloader);
        pageFetched = true;
    }

    protected void assertPageFetched() {
        if (!pageFetched) throw new IllegalStateException(
                "Page is not fetched. Make sure you call fetchPage()");
    }

    protected boolean isPageFetched() { return pageFetched; }

    public abstract void onFetchPage(@Nonnull Downloader downloader)
            throws IOException, ExtractionException;

    @Nonnull public String getId() throws ParsingException { return linkHandler.getId(); }
    @Nonnull public abstract String getName() throws ParsingException;
    @Nonnull public String getOriginalUrl() throws ParsingException {
        return linkHandler.getOriginalUrl();
    }
    @Nonnull public String getUrl() throws ParsingException { return linkHandler.getUrl(); }
    @Nonnull public String getBaseUrl() throws ParsingException { return linkHandler.getBaseUrl(); }
    @Nonnull public StreamingService getService() { return service; }
    public int getServiceId() { return service.getServiceId(); }
    public Downloader getDownloader() { return downloader; }

    public void forceLocalization(final Localization localization) {
        forcedLocalization = localization;
    }

    public void forceContentCountry(final ContentCountry contentCountry) {
        forcedContentCountry = contentCountry;
    }

    @Nonnull public Localization getExtractorLocalization() {
        return forcedLocalization == null ? service.getLocalization() : forcedLocalization;
    }

    @Nonnull public ContentCountry getExtractorContentCountry() {
        return forcedContentCountry == null ? service.getContentCountry() : forcedContentCountry;
    }

    @Nonnull public TimeAgoParser getTimeAgoParser() {
        return service.getTimeAgoParser(getExtractorLocalization());
    }

    @Override public String toString() { return getClass().getSimpleName(); }
}
