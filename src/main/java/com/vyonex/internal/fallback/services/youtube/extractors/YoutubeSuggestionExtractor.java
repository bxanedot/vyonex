/*
 * Created by Christian Schabesberger on 28.09.16.
 *
 * Copyright (C) 2015 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeSuggestionExtractor.java is part of VyonEx.
 *
 * VyonEx is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VyonEx is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VyonEx.  If not, see <https://www.gnu.org/licenses/>.
 */

// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import static com.vyonex.internal.fallback.utils.Utils.isBlank;
import static com.vyonex.internal.fallback.utils.Utils.isNullOrEmpty;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor;
import com.vyonex.internal.fallback.utils.Utils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class YoutubeSuggestionExtractor extends SuggestionExtractor {

    public YoutubeSuggestionExtractor(final StreamingService service) {
        super(service);
    }

    @Override
    public List<String> suggestionList(final String query) throws IOException, ExtractionException {
        final String url = "https://suggestqueries-clients6.youtube.com/complete/search"
                + "?client=" + "youtube"
                + "&ds=" + "yt"
                + "&gl=" + Utils.encodeUrlUtf8(getExtractorContentCountry().getCountryCode())
                + "&q=" + Utils.encodeUrlUtf8(query)
                + "&xhr=t";

        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("Origin", Collections.singletonList("https://www.youtube.com"));
        headers.put("Referer", Collections.singletonList("https://www.youtube.com"));

        final Response response = VyonEx.getDownloader()
                .get(url, headers, getExtractorLocalization());

        final String contentTypeHeader = response.getHeader("Content-Type");
        if (isNullOrEmpty(contentTypeHeader) || !contentTypeHeader.contains("application/json")) {
            throw new ExtractionException("Invalid response type (got \"" + contentTypeHeader
                    + "\", excepted a JSON response) (response code "
                    + response.responseCode() + ")");
        }

        final String responseBody = response.responseBody();

        if (responseBody.isEmpty()) {
            throw new ExtractionException("Empty response received");
        }

        try {
            final JsonArray suggestions = JsonParser.array()
                    .from(responseBody)
                    .getArray(1); // 0: search query, 1: search suggestions, 2: tracking data?
            return suggestions.stream()
                    .filter(JsonArray.class::isInstance)
                    .map(JsonArray.class::cast)
                    .map(suggestion -> suggestion.getString(0)) // 0 is the search suggestion
                    .filter(suggestion -> !isBlank(suggestion)) // Filter blank suggestions
                    .collect(Collectors.toUnmodifiableList());
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse JSON response", e);
        }
    }
}
