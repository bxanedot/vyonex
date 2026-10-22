// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import static com.vyonex.internal.fallback.utils.Utils.isBlank;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
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
    private static final String SUGGESTION_URL =
            "https://suggestqueries-clients6.youtube.com/complete/search";

    public YoutubeSuggestionExtractor(final StreamingService service) {
        super(service);
    }

    @Override
    public List<String> suggestionList(final String query) throws IOException, ExtractionException {
        final String url = SUGGESTION_URL + "?client=youtube&ds=yt&gl="
                + Utils.encodeUrlUtf8(getExtractorContentCountry().getCountryCode())
                + "&q=" + Utils.encodeUrlUtf8(query) + "&xhr=t";
        final Map<String, List<String>> headers = Map.of(
                "Origin", List.of("https://www.youtube.com"),
                "Referer", List.of("https://www.youtube.com"));
        final Response response = VyonEx.getDownloader().get(
                url, headers, getExtractorLocalization());
        final String contentType = response.getHeader("Content-Type");
        if (contentType == null
                || !contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
            throw new ExtractionException("Expected JSON suggestions, received " + contentType
                    + " (HTTP " + response.responseCode() + ")");
        }
        final String body = response.responseBody();
        if (body.isEmpty()) throw new ExtractionException("Empty suggestions response");

        try {
            final JsonArray items = JsonParser.array().from(body).getArray(1);
            final List<String> suggestions = new ArrayList<>(items.size());
            for (final Object item : items) {
                if (!(item instanceof JsonArray)) continue;
                final String value = ((JsonArray) item).getString(0);
                if (!isBlank(value)) suggestions.add(value);
            }
            return List.copyOf(suggestions);
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse suggestion response", e);
        }
    }
}
