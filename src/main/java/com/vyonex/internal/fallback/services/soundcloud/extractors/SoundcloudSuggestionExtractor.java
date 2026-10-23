// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SoundcloudSuggestionExtractor extends SuggestionExtractor {
    public SoundcloudSuggestionExtractor(final StreamingService service) { super(service); }

    @Override public List<String> suggestionList(final String query)
            throws IOException, ExtractionException {
        final String url = SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL + "search/queries?q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&client_id=" + SoundcloudParsingHelper.clientId() + "&limit=10";
        final Response response = VyonEx.getDownloader().get(url, getExtractorLocalization());
        if (response.responseCode() >= 400) {
            throw new IOException("SoundCloud suggestions failed with HTTP "
                    + response.responseCode());
        }
        try {
            final List<String> suggestions = new ArrayList<>();
            for (final Object item : JsonParser.object().from(response.responseBody())
                    .getArray("collection")) {
                if (item instanceof JsonObject) {
                    final String text = ((JsonObject) item).getString("query");
                    if (text != null) suggestions.add(text);
                }
            }
            return suggestions;
        } catch (final JsonParserException e) {
            throw new ParsingException("Invalid SoundCloud suggestions response", e);
        }
    }
}
