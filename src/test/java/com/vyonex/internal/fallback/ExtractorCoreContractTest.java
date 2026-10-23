// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractorCoreContractTest {
    @Test
    void fetchesOnceAndCanRetryAfterFailure() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        VyonEx.init(offlineDownloader());
        try {
            final Probe extractor = new Probe();
            assertThrows(IllegalStateException.class, extractor::requireFetched);
            extractor.failNextFetch = true;
            assertThrows(IOException.class, extractor::fetchPage);
            assertFalse(extractor.fetched());

            extractor.fetchPage();
            extractor.fetchPage();
            assertTrue(extractor.fetched());
            assertEquals(2, extractor.fetchCalls);
        } finally {
            VyonEx.init(previous);
        }
    }

    @Test
    void keepsContinuationAndEmptyPageContracts() {
        final byte[] body = {1, 2};
        final Page continuation = new Page("https://example.test/page", "next", body);
        assertTrue(Page.isValid(continuation));
        assertEquals("next", continuation.getId());
        assertEquals(body, continuation.getBody());
        assertTrue(Page.isValid(new Page(List.of("item-1"))));
        assertFalse(Page.isValid(new Page((String) null)));

        final ListExtractor.InfoItemsPage<InfoItem> empty =
                ListExtractor.InfoItemsPage.emptyPage();
        assertTrue(empty.getItems().isEmpty());
        assertTrue(empty.getErrors().isEmpty());
        assertFalse(empty.hasNextPage());
    }

    @Test
    void forcedLocaleAndCountryOverrideServiceDefaults() {
        final Downloader previous = VyonEx.getDownloader();
        VyonEx.init(offlineDownloader());
        try {
            final Probe extractor = new Probe();
            final Localization language = new Localization("fr", "CH");
            final ContentCountry country = new ContentCountry("CA");
            extractor.forceLocalization(language);
            extractor.forceContentCountry(country);
            assertEquals(language, extractor.getExtractorLocalization());
            assertEquals(country, extractor.getExtractorContentCountry());
        } finally {
            VyonEx.init(previous);
        }
    }

    private static Downloader offlineDownloader() {
        return new Downloader() {
            @Override public Response execute(final Request request)
                    throws IOException, ReCaptchaException {
                throw new IOException("Unexpected request in extractor contract test");
            }
        };
    }

    private static final class Probe extends Extractor {
        private int fetchCalls;
        private boolean failNextFetch;

        private Probe() {
            super(new MediaCCCService(64),
                    new LinkHandler("https://media.ccc.de/v/test",
                            "https://media.ccc.de/v/test", "test"));
        }

        @Override public void onFetchPage(final Downloader downloader) throws IOException {
            fetchCalls++;
            if (failNextFetch) {
                failNextFetch = false;
                throw new IOException("Offline retry check");
            }
        }

        @Override public String getName() { return "test"; }
        private boolean fetched() { return isPageFetched(); }
        private void requireFetched() { assertPageFetched(); }
    }
}
