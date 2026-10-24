// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.extractors

import com.vyonex.internal.fallback.StreamingService
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor

public class PeertubeSuggestionExtractor(service: StreamingService) : SuggestionExtractor(service) {
    override fun suggestionList(query: String): List<String> = emptyList()
}
