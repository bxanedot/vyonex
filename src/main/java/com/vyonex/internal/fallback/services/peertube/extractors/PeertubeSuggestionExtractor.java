// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor;

import java.util.List;

public final class PeertubeSuggestionExtractor extends SuggestionExtractor {
    public PeertubeSuggestionExtractor(final StreamingService service) {
        super(service);
    }

    @Override public List<String> suggestionList(final String query) { return List.of(); }
}
