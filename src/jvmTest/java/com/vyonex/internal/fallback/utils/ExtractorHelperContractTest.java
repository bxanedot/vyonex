// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import com.vyonex.internal.fallback.Info;
import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.ListExtractor.InfoItemsPage;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.peertube.PeertubeInstance;
import com.vyonex.internal.fallback.services.peertube.PeertubeService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ExtractorHelperContractTest {
    @Test
    void returnsPageAndCarriesCollectorErrorsIntoInfo() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        VyonEx.init(new Downloader() {
            @Override
            public Response execute(final Request request) throws IOException, ReCaptchaException {
                throw new IOException("Unexpected request");
            }
        });
        try {
            final RuntimeException pageError = new RuntimeException("item issue");
            final InfoItem item = new InfoItem(InfoItem.InfoType.STREAM, 3, "url", "name") { };
            final InfoItemsPage<InfoItem> sourcePage = new InfoItemsPage<>(
                    List.of(item), null, List.of(pageError));
            final TestExtractor extractor = new TestExtractor(sourcePage, false);
            final Info info = new Info(3, "id", "url", "url", "test") { };

            final InfoItemsPage<InfoItem> result =
                    ExtractorHelper.getItemsPageOrLogError(info, extractor);

            assertSame(sourcePage, result);
            assertEquals(List.of(pageError), info.getErrors());
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void convertsPageFailureIntoAnEmptyPageAndRecordedError() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        VyonEx.init(new Downloader() {
            @Override
            public Response execute(final Request request) throws IOException, ReCaptchaException {
                throw new IOException("Unexpected request");
            }
        });
        try {
            final TestExtractor extractor = new TestExtractor(null, true);
            final Info info = new Info(3, "id", "url", "url", "test") { };

            final InfoItemsPage<InfoItem> result =
                    ExtractorHelper.getItemsPageOrLogError(info, extractor);

            assertEquals(List.of(), result.getItems());
            assertEquals("page failed", info.getErrors().get(0).getMessage());
        } finally {
            VyonEx.init(previous);
        }
    }

    private static final class TestExtractor extends ListExtractor<InfoItem> {
        private final InfoItemsPage<InfoItem> page;
        private final boolean fail;

        private TestExtractor(final InfoItemsPage<InfoItem> page, final boolean fail) {
            super(new PeertubeService(3, new PeertubeInstance("https://tube.example", "test")),
                    new ListLinkHandler("url", "url", "id", List.of(), ""));
            this.page = page;
            this.fail = fail;
        }

        @Override
        public InfoItemsPage<InfoItem> getInitialPage() {
            if (fail) throw new IllegalStateException("page failed");
            return page;
        }

        @Override public InfoItemsPage<InfoItem> getPage(final Page page) { return this.page; }
        @Override public String getName() { return "test"; }
        @Override public void onFetchPage(final Downloader downloader) { }
    }
}
