// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class MediaCCCParsingHelper {
    private static final Pattern LIVE_ID = Pattern.compile("[A-Za-z0-9_-]+/[A-Za-z0-9_-]+");

    private MediaCCCParsingHelper() { }

    public static boolean isLiveStreamId(final String id) {
        return id != null && LIVE_ID.matcher(id).matches();
    }

    public static JsonArray getLiveStreams(final Downloader downloader,
                                           final Localization localization)
            throws ExtractionException {
        try {
            final Response response = downloader.get(
                    "https://streaming.media.ccc.de/streams/v2.json", localization);
            if (response == null) throw new ExtractionException("No live stream response received");
            if (response.responseCode() < 200 || response.responseCode() >= 300) {
                throw new ExtractionException("Live stream request failed with HTTP "
                        + response.responseCode());
            }
            return JsonParser.array().from(response.responseBody());
        } catch (final IOException | ReCaptchaException error) {
            throw new ExtractionException("Could not fetch live stream data.", error);
        } catch (final JsonParserException error) {
            throw new ExtractionException("Could not parse live stream data.", error);
        }
    }

    public static JsonObject fetchJsonObject(final Downloader downloader, final String url,
                                             @Nullable final Localization localization)
            throws IOException, ExtractionException {
        final Response response = localization == null ? downloader.get(url)
                : downloader.get(url, localization);
        if (response == null) throw new ExtractionException("No API response received from " + url);
        if (response.responseCode() < 200 || response.responseCode() >= 300) {
            throw new ExtractionException("API request failed with HTTP "
                    + response.responseCode() + " for " + url);
        }
        try {
            return JsonParser.object().from(response.responseBody());
        } catch (final JsonParserException error) {
            throw new ExtractionException("Could not parse API response from " + url, error);
        }
    }

    public static List<Image> getImageListFromLogoImageUrl(final String url) {
        return url == null || url.isEmpty() ? List.of()
                : List.of(new Image(url, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN,
                        Image.ResolutionLevel.UNKNOWN));
    }

    public static List<Image> getThumbnailsFromStreamItem(final JsonObject item) {
        return thumbnails(item, "thumb_url", "poster_url");
    }

    public static List<Image> getThumbnailsFromLiveStreamItem(final JsonObject item) {
        return thumbnails(item, "thumb", "poster");
    }

    private static List<Image> thumbnails(final JsonObject item, final String previewKey,
                                          final String posterKey) {
        final List<Image> images = new ArrayList<>(2);
        appendImage(images, item.getString(previewKey), Image.ResolutionLevel.MEDIUM);
        appendImage(images, item.getString(posterKey), Image.ResolutionLevel.HIGH);
        return List.copyOf(images);
    }

    private static void appendImage(final List<Image> images, final String url,
                                    final Image.ResolutionLevel quality) {
        if (url != null && !url.isEmpty()) {
            images.add(new Image(url, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, quality));
        }
    }
}
