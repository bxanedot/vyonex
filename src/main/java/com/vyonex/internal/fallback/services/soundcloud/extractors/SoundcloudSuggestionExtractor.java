// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import static com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.suggestion.SuggestionExtractor;
import com.vyonex.internal.fallback.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SoundcloudSuggestionExtractor extends SuggestionExtractor {

    public SoundcloudSuggestionExtractor(final StreamingService service) {
        super(service);
    }

    @Override
    public List<String> suggestionList(final String query) throws IOException,
            ExtractionException {
        final List<String> suggestions = new ArrayList<>();
        final Downloader dl = VyonEx.getDownloader();
        final String url = SOUNDCLOUD_API_V2_URL + "search/queries?q="
                + Utils.encodeUrlUtf8(query) + "&client_id=" + SoundcloudParsingHelper.clientId()
                + "&limit=10";
        final String response = dl.get(url, getExtractorLocalization()).responseBody();

        try {
            final JsonArray collection = JsonParser.object().from(response).getArray("collection");
            for (final Object suggestion : collection) {
                if (suggestion instanceof JsonObject) {
                    suggestions.add(((JsonObject) suggestion).getString("query"));
                }
            }

            return suggestions;
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }
    }
}
