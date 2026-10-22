// Created by Fynn Godau 2019, licensed GNU GPL version 3 or later

// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors;

import static com.vyonex.internal.fallback.services.bandcamp.extractors.BandcampExtractorHelper.BASE_API_URL;

import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import com.vyonex.internal.json.JsonWriter;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BandcampSuggestionExtractor extends SuggestionExtractor {

    private static final String AUTOCOMPLETE_URL = BASE_API_URL
            + "/bcsearch_public_api/1/autocomplete_elastic";
    public BandcampSuggestionExtractor(final StreamingService service) {
        super(service);
    }

    @Override
    public List<String> suggestionList(final String query) throws IOException, ExtractionException {
        final Downloader downloader = VyonEx.getDownloader();

        try {
            final JsonObject fuzzyResults = JsonParser.object().from(downloader
                    .postWithContentTypeJson(
                            AUTOCOMPLETE_URL,
                            Collections.emptyMap(),
                            JsonWriter.string()
                                    .object()
                                    .value("fan_id", (String) null)
                                    .value("full_page", false)
                                    .value("search_filter", "")
                                    .value("search_text", query)
                                    .end()
                                    .done()
                                    .getBytes(StandardCharsets.UTF_8)).responseBody());

            return fuzzyResults.getObject("auto").getArray("results")
                    .streamAsJsonObjects()
                    .map(jsonObject -> jsonObject.getString("name"))
                    .distinct()
                    .collect(Collectors.toList());
        } catch (final JsonParserException e) {
            return Collections.emptyList();
        }
    }
}
