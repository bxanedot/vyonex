// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.suggestion;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public abstract class SuggestionExtractor {
    private final StreamingService service;
    @Nullable private Localization forcedLocalization;
    @Nullable private ContentCountry forcedContentCountry;

    public SuggestionExtractor(final StreamingService service) {
        this.service = service;
    }

    public abstract List<String> suggestionList(String query)
            throws IOException, ExtractionException;

    public int getServiceId() { return service.getServiceId(); }
    public StreamingService getService() { return service; }

    public void forceLocalization(@Nullable final Localization localization) {
        forcedLocalization = localization;
    }

    public void forceContentCountry(@Nullable final ContentCountry contentCountry) {
        forcedContentCountry = contentCountry;
    }

    @Nonnull
    public Localization getExtractorLocalization() {
        return forcedLocalization == null ? service.getLocalization() : forcedLocalization;
    }

    @Nonnull
    public ContentCountry getExtractorContentCountry() {
        return forcedContentCountry == null ? service.getContentCountry() : forcedContentCountry;
    }
}
