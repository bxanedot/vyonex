// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.kiosk;

import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandlerFactory;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

public class KioskList {
    public interface KioskExtractorFactory {
        KioskExtractor createNewKiosk(StreamingService service, String url, String kioskId)
                throws ExtractionException, IOException;
    }

    private static final class KioskEntry {
        private final KioskExtractorFactory extractorFactory;
        private final ListLinkHandlerFactory handlerFactory;

        private KioskEntry(final KioskExtractorFactory extractorFactory,
                           final ListLinkHandlerFactory handlerFactory) {
            this.extractorFactory = extractorFactory;
            this.handlerFactory = handlerFactory;
        }
    }

    private final StreamingService service;
    private final HashMap<String, KioskEntry> kiosks = new HashMap<>();
    private String defaultKiosk;
    @Nullable private Localization forcedLocalization;
    @Nullable private ContentCountry forcedContentCountry;

    public KioskList(final StreamingService service) {
        this.service = service;
    }

    public void addKioskEntry(final KioskExtractorFactory extractorFactory,
                              final ListLinkHandlerFactory handlerFactory,
                              final String id) throws Exception {
        if (kiosks.containsKey(id)) {
            throw new Exception("Kiosk with type " + id + " already exists.");
        }
        kiosks.put(id, new KioskEntry(extractorFactory, handlerFactory));
    }

    public void setDefaultKiosk(final String kioskType) { defaultKiosk = kioskType; }

    public KioskExtractor getDefaultKioskExtractor()
            throws ExtractionException, IOException {
        return getDefaultKioskExtractor(null);
    }

    public KioskExtractor getDefaultKioskExtractor(final Page nextPage)
            throws ExtractionException, IOException {
        return getDefaultKioskExtractor(nextPage, VyonEx.getPreferredLocalization());
    }

    public KioskExtractor getDefaultKioskExtractor(final Page nextPage,
                                                   final Localization localization)
            throws ExtractionException, IOException {
        if (!isNullOrEmpty(defaultKiosk)) {
            return getExtractorById(defaultKiosk, nextPage, localization);
        }
        final String firstId = kiosks.keySet().stream().findAny().orElse(null);
        return firstId == null ? null : getExtractorById(firstId, nextPage, localization);
    }

    public String getDefaultKioskId() { return defaultKiosk; }

    public KioskExtractor getExtractorById(final String kioskId, final Page nextPage)
            throws ExtractionException, IOException {
        return getExtractorById(kioskId, nextPage, VyonEx.getPreferredLocalization());
    }

    public KioskExtractor getExtractorById(final String kioskId,
                                           final Page nextPage,
                                           @Nullable final Localization localization)
            throws ExtractionException, IOException {
        final KioskEntry entry = kiosks.get(kioskId);
        if (entry == null) {
            throw new ExtractionException("No kiosk found with the type: " + kioskId);
        }

        final String url = entry.handlerFactory.fromId(kioskId).getUrl();
        final KioskExtractor extractor = entry.extractorFactory.createNewKiosk(
                service, url, kioskId);
        if (localization != null) extractor.forceLocalization(localization);
        if (forcedLocalization != null) extractor.forceLocalization(forcedLocalization);
        if (forcedContentCountry != null) extractor.forceContentCountry(forcedContentCountry);
        return extractor;
    }

    public Set<String> getAvailableKiosks() { return kiosks.keySet(); }

    public KioskExtractor getExtractorByUrl(final String url, final Page nextPage)
            throws ExtractionException, IOException {
        return getExtractorByUrl(url, nextPage, VyonEx.getPreferredLocalization());
    }

    public KioskExtractor getExtractorByUrl(final String url,
                                            final Page nextPage,
                                            @Nullable final Localization localization)
            throws ExtractionException, IOException {
        for (final Map.Entry<String, KioskEntry> candidate : kiosks.entrySet()) {
            final ListLinkHandlerFactory factory = candidate.getValue().handlerFactory;
            if (factory.acceptUrl(url)) {
                return getExtractorById(factory.getId(url), nextPage, localization);
            }
        }
        throw new ExtractionException("Could not find a kiosk that fits to the url: " + url);
    }

    public ListLinkHandlerFactory getListLinkHandlerFactoryByType(final String type) {
        return kiosks.get(type).handlerFactory;
    }

    public void forceLocalization(@Nullable final Localization localization) {
        forcedLocalization = localization;
    }

    public void forceContentCountry(@Nullable final ContentCountry contentCountry) {
        forcedContentCountry = contentCountry;
    }
}
