// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import com.vyonex.internal.json.JsonWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class BandcampSuggestionExtractor extends SuggestionExtractor {
    private static final String AUTOCOMPLETE_URL = BandcampExtractorHelper.BASE_API_URL
            + "/bcsearch_public_api/1/autocomplete_elastic";

    public BandcampSuggestionExtractor(final StreamingService service) {
        super(service);
    }

    @Override
    public List<String> suggestionList(final String query) throws IOException, ExtractionException {
        final Downloader downloader = VyonEx.getDownloader();
        if (downloader == null) throw new ParsingException("VyonEx has not been initialized");

        final byte[] body = JsonWriter.string().object()
                .value("fan_id", (String) null)
                .value("full_page", false)
                .value("search_filter", "")
                .value("search_text", query)
                .end().done().getBytes(StandardCharsets.UTF_8);

        try {
            final JsonObject response = JsonParser.object().from(downloader
                    .postWithContentTypeJson(AUTOCOMPLETE_URL, Map.of(), body).responseBody());
            final JsonArray results = response.getObject("auto").getArray("results");
            final LinkedHashSet<String> names = new LinkedHashSet<>();
            for (final Object item : results) {
                if (!(item instanceof JsonObject result)) continue;
                final String name = result.getString("name", "");
                if (!name.isBlank()) names.add(name);
            }
            return new ArrayList<>(names);
        } catch (final JsonParserException e) {
            return List.of();
        } catch (final ReCaptchaException e) {
            throw new ParsingException("Bandcamp blocked autocomplete", e);
        }
    }
}
