// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class YoutubeSuggestionExtractor extends SuggestionExtractor {
    private static final String ENDPOINT =
            "https://suggestqueries-clients6.youtube.com/complete/search";
    private static final Map<String, List<String>> REQUEST_HEADERS = Map.of(
            "Origin", List.of("https://www.youtube.com"),
            "Referer", List.of("https://www.youtube.com"));

    public YoutubeSuggestionExtractor(final StreamingService service) {
        super(service);
    }

    @Override
    public List<String> suggestionList(final String query) throws IOException, ExtractionException {
        final Downloader downloader = VyonEx.getDownloader();
        if (downloader == null) throw new ExtractionException("No downloader is configured");

        final String url = ENDPOINT + "?client=youtube&ds=yt&gl="
                + Utils.encodeUrlUtf8(getExtractorContentCountry().getCountryCode())
                + "&q=" + Utils.encodeUrlUtf8(query) + "&xhr=t";
        final Response response = downloader.get(url, REQUEST_HEADERS, getExtractorLocalization());
        final String contentType = response.getHeader("Content-Type");
        if (response.responseCode() != 200 || contentType == null
                || !contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
            throw new ExtractionException("Expected JSON suggestions, received " + contentType
                    + " (HTTP " + response.responseCode() + ")");
        }
        if (response.responseBody().isEmpty()) {
            throw new ExtractionException("Empty suggestions response");
        }

        try {
            final JsonArray rows = JsonParser.array().from(response.responseBody()).getArray(1);
            final List<String> suggestions = new ArrayList<>(rows.size());
            for (final Object row : rows) {
                if (!(row instanceof JsonArray)) continue;
                final String suggestion = ((JsonArray) row).getString(0);
                if (suggestion != null && !suggestion.isBlank()) suggestions.add(suggestion);
            }
            return List.copyOf(suggestions);
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse suggestion response", e);
        }
    }
}
