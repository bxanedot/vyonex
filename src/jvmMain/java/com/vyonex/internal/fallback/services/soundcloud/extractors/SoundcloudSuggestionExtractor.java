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
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudSearchUrl;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudSuggestionParser;
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor;

import java.io.IOException;
import java.util.List;

public final class SoundcloudSuggestionExtractor extends SuggestionExtractor {
    public SoundcloudSuggestionExtractor(final StreamingService service) { super(service); }

    @Override public List<String> suggestionList(final String query)
            throws IOException, ExtractionException {
        final String url = SoundcloudSearchUrl.INSTANCE.suggestionsUrl(
                query, SoundcloudParsingHelper.clientId());
        final Response response = VyonEx.getDownloader().get(url, getExtractorLocalization());
        if (response.responseCode() >= 400) {
            throw new IOException("SoundCloud suggestions failed with HTTP "
                    + response.responseCode());
        }
        try {
            return SoundcloudSuggestionParser.INSTANCE.parse(response.responseBody());
        } catch (final IllegalArgumentException e) {
            throw new ParsingException("Invalid SoundCloud suggestions response", e);
        }
    }
}
