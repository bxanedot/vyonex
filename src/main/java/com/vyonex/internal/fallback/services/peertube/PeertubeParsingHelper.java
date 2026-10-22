// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PeertubeParsingHelper {
    public static final String START_KEY = "start";
    public static final String COUNT_KEY = "count";
    public static final int ITEMS_PER_PAGE = 12;
    private static final Pattern START_PARAM = Pattern.compile("([?&]start=)(\\d+)(?=&|#|$)");

    private PeertubeParsingHelper() { }

    public static void validate(final JsonObject json) throws ContentNotAvailableException {
        final String error = json.getString("error");
        if (error != null && !error.isBlank()) throw new ContentNotAvailableException(error);
    }

    public static Page getNextPage(final String previousUrl, final long total) {
        final Matcher match = START_PARAM.matcher(previousUrl);
        if (!match.find()) return null;
        try {
            final long next = Math.addExact(Long.parseLong(match.group(2)), ITEMS_PER_PAGE);
            return next >= total ? null : new Page(match.replaceFirst(
                    Matcher.quoteReplacement(match.group(1) + next)));
        } catch (final ArithmeticException | NumberFormatException e) {
            return null;
        }
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

        for (final Object value : (JsonArray) data) {
            if (!(value instanceof JsonObject)) continue;
            JsonObject item = (JsonObject) value;
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
        return getImages(baseUrl, item, "avatars", "avatar");
    }

    public static List<Image> getBannersFromAccountOrVideoChannelObject(
            final String baseUrl, final JsonObject item) {
        return getImages(baseUrl, item, "banners", "banner");
    }

    public static List<Image> getThumbnailsFromPlaylistOrVideoItem(
            final String baseUrl, final JsonObject item) {
        final List<Image> images = new ArrayList<>(2);
        addThumbnail(images, baseUrl, item.getString("thumbnailPath"), ResolutionLevel.LOW);
        addThumbnail(images, baseUrl, item.getString("previewPath"), ResolutionLevel.MEDIUM);
        return Collections.unmodifiableList(images);
    }

    private static List<Image> getImages(final String baseUrl, final JsonObject item,
                                         final String arrayKey, final String objectKey) {
        final JsonArray entries = item.getArray(arrayKey);
        if (!entries.isEmpty()) {
            final List<Image> images = new ArrayList<>(entries.size());
            for (final Object value : entries) {
                if (value instanceof JsonObject) addProfileImage(images, baseUrl, (JsonObject) value);
            }
            return Collections.unmodifiableList(images);
        }

        final List<Image> fallback = new ArrayList<>(1);
        addProfileImage(fallback, baseUrl, item.getObject(objectKey));
        return Collections.unmodifiableList(fallback);
    }

    private static void addProfileImage(final List<Image> images, final String baseUrl,
                                        final JsonObject image) {
        final String path = image.getString("path");
        if (path != null && !path.isBlank()) {
            images.add(new Image(resolve(baseUrl, path), Image.HEIGHT_UNKNOWN,
                    image.getInt("width", Image.WIDTH_UNKNOWN), ResolutionLevel.UNKNOWN));
        }
    }

    private static void addThumbnail(final List<Image> images, final String baseUrl,
                                     final String path, final ResolutionLevel level) {
        if (path != null && !path.isBlank()) {
            images.add(new Image(resolve(baseUrl, path), Image.HEIGHT_UNKNOWN,
                    Image.WIDTH_UNKNOWN, level));
        }
    }

    private static String resolve(final String baseUrl, final String path) {
        try {
            return URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/")
                    .resolve(path).toString();
        } catch (final IllegalArgumentException e) {
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
