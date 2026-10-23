// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.Image.ResolutionLevel;
import com.vyonex.internal.fallback.InfoItemsCollector;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeChannelInfoItemExtractor;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubePlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeSepiaStreamInfoItemExtractor;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeStreamInfoItemExtractor;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PeertubeParsingHelper {
    public static final String START_KEY = "start";
    public static final String COUNT_KEY = "count";
    public static final int ITEMS_PER_PAGE = 12;

    private PeertubeParsingHelper() { }

    public static void validate(final JsonObject response) throws ContentNotAvailableException {
        final String message = response.getString("error");
        if (message != null && !message.isBlank()) throw new ContentNotAvailableException(message);
    }

    public static Page getNextPage(final String previousUrl, final long total) {
        final int queryStart = previousUrl.indexOf('?');
        if (queryStart < 0) return null;
        final int fragmentStart = previousUrl.indexOf('#', queryStart);
        final int queryEnd = fragmentStart < 0 ? previousUrl.length() : fragmentStart;
        int parameterStart = queryStart + 1;
        while (parameterStart < queryEnd) {
            final int ampersand = previousUrl.indexOf('&', parameterStart);
            final int parameterEnd = ampersand < 0 || ampersand > queryEnd
                    ? queryEnd : ampersand;
            final int equals = previousUrl.indexOf('=', parameterStart);
            final int nameEnd = equals < 0 || equals >= parameterEnd ? parameterEnd : equals;
            if (START_KEY.equals(previousUrl.substring(parameterStart, nameEnd))) {
                if (nameEnd == parameterEnd) return null;
                try {
                    final String encodedStart = previousUrl.substring(nameEnd + 1, parameterEnd);
                    final long currentStart = Long.parseLong(
                            URLDecoder.decode(encodedStart, StandardCharsets.UTF_8));
                    if (currentStart < 0) return null;
                    final long nextStart = Math.addExact(currentStart, ITEMS_PER_PAGE);
                    return nextStart >= total ? null : new Page(previousUrl.substring(0, nameEnd + 1)
                            + nextStart + previousUrl.substring(parameterEnd));
                } catch (final IllegalArgumentException | ArithmeticException ignored) {
                    return null;
                }
            }
            parameterStart = parameterEnd + 1;
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public static void collectItemsFrom(final InfoItemsCollector collector,
                                        final JsonObject response,
                                        final String baseUrl) throws ParsingException {
        collectItemsFrom(collector, response, baseUrl, false);
    }

    @SuppressWarnings("rawtypes")
    public static void collectItemsFrom(final InfoItemsCollector collector,
                                        final JsonObject response,
                                        final String baseUrl,
                                        final boolean sepia) throws ParsingException {
        final Object data = response.get("data");
        if (!(data instanceof JsonArray)) throw new ParsingException("Response data is not an array");

        for (final Object entry : (JsonArray) data) {
            if (!(entry instanceof JsonObject)) continue;
            JsonObject item = (JsonObject) entry;
            if (item.get("video") instanceof JsonObject) item = item.getObject("video");

            if (sepia) {
                collector.commit(new PeertubeSepiaStreamInfoItemExtractor(item, baseUrl));
            } else if (item.has("videosLength")) {
                collector.commit(new PeertubePlaylistInfoItemExtractor(item, baseUrl));
            } else if (item.has("followersCount")) {
                collector.commit(new PeertubeChannelInfoItemExtractor(item, baseUrl));
            } else {
                collector.commit(new PeertubeStreamInfoItemExtractor(item, baseUrl));
            }
        }
    }

    public static String requiredString(final JsonObject item, final String path)
            throws ParsingException {
        final Object value = valueAt(item, path);
        if (value instanceof String) return (String) value;
        throw new ParsingException("Missing string field: " + path);
    }

    public static String optionalString(final JsonObject item, final String path) {
        final Object value = valueAt(item, path);
        return value instanceof String ? (String) value : null;
    }

    public static Number requiredNumber(final JsonObject item, final String path)
            throws ParsingException {
        final Object value = valueAt(item, path);
        if (value instanceof Number) return (Number) value;
        throw new ParsingException("Missing numeric field: " + path);
    }

    public static List<Image> getAvatarsFromOwnerAccountOrVideoChannelObject(
            final String baseUrl, final JsonObject item) {
        return profileImages(baseUrl, item, "avatars", "avatar");
    }

    public static List<Image> getBannersFromAccountOrVideoChannelObject(
            final String baseUrl, final JsonObject item) {
        return profileImages(baseUrl, item, "banners", "banner");
    }

    public static List<Image> getThumbnailsFromPlaylistOrVideoItem(
            final String baseUrl, final JsonObject item) {
        final List<Image> thumbnails = new ArrayList<>(2);
        addThumbnail(thumbnails, baseUrl, item.getString("thumbnailPath"), ResolutionLevel.LOW);
        addThumbnail(thumbnails, baseUrl, item.getString("previewPath"), ResolutionLevel.MEDIUM);
        return Collections.unmodifiableList(thumbnails);
    }

    private static List<Image> profileImages(final String baseUrl, final JsonObject item,
                                             final String listKey, final String fallbackKey) {
        final JsonArray values = item.getArray(listKey);
        final List<Image> images = new ArrayList<>(values.isEmpty() ? 1 : values.size());
        if (values.isEmpty()) {
            addProfileImage(images, baseUrl, item.getObject(fallbackKey));
        } else {
            for (final Object value : values) {
                if (value instanceof JsonObject) addProfileImage(images, baseUrl, (JsonObject) value);
            }
        }
        return Collections.unmodifiableList(images);
    }

    private static void addProfileImage(final List<Image> images, final String baseUrl,
                                        final JsonObject value) {
        final String path = value.getString("path");
        if (path != null && !path.isBlank()) {
            images.add(new Image(resolve(baseUrl, path), Image.HEIGHT_UNKNOWN,
                    value.getInt("width", Image.WIDTH_UNKNOWN), ResolutionLevel.UNKNOWN));
        }
    }

    private static void addThumbnail(final List<Image> images, final String baseUrl,
                                     final String path, final ResolutionLevel quality) {
        if (path != null && !path.isBlank()) {
            images.add(new Image(resolve(baseUrl, path), Image.HEIGHT_UNKNOWN,
                    Image.WIDTH_UNKNOWN, quality));
        }
    }

    private static String resolve(final String baseUrl, final String path) {
        try {
            final String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            return URI.create(base).resolve(path).toString();
        } catch (final IllegalArgumentException ignored) {
            return baseUrl + (path.startsWith("/") ? "" : "/") + path;
        }
    }

    private static Object valueAt(final JsonObject item, final String path) {
        Object value = item;
        for (final String key : path.split("\\.")) {
            if (!(value instanceof JsonObject)) return null;
            value = ((JsonObject) value).get(key);
        }
        return value;
    }
}
