// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/** Shared JSON value readers used by VyonEx extractors. */
public final class JsonUtils {
    private JsonUtils() { }

    @Nonnull
    public static Object getValue(@Nonnull final JsonObject object,
                                  @Nonnull final String path) throws ParsingException {
        final String[] keys = path.split("\\.");
        JsonObject current = object;
        for (int index = 0; index < keys.length - 1; index++) {
            current = current.getObject(keys[index], null);
            if (current == null) {
                throw missingValue(path);
            }
        }

        final Object value = current.get(keys[keys.length - 1]);
        if (value == null) {
            throw missingValue(path);
        }
        return value;
    }

    private static ParsingException missingValue(final String path) {
        return new ParsingException("Unable to get " + path);
    }

    private static <T> T getTypedValue(@Nonnull final JsonObject object,
                                       @Nonnull final String path,
                                       @Nonnull final Class<T> type) throws ParsingException {
        final Object value = getValue(object, path);
        if (!type.isInstance(value)) {
            throw new ParsingException("Wrong data type at path " + path);
        }
        return type.cast(value);
    }

    @Nonnull
    public static String getString(@Nonnull final JsonObject object, @Nonnull final String path)
            throws ParsingException {
        return getTypedValue(object, path, String.class);
    }

    @Nonnull
    public static Boolean getBoolean(@Nonnull final JsonObject object,
                                     @Nonnull final String path) throws ParsingException {
        return getTypedValue(object, path, Boolean.class);
    }

    @Nonnull
    public static Number getNumber(@Nonnull final JsonObject object,
                                   @Nonnull final String path) throws ParsingException {
        return getTypedValue(object, path, Number.class);
    }

    @Nonnull
    public static JsonObject getObject(@Nonnull final JsonObject object,
                                       @Nonnull final String path) throws ParsingException {
        return getTypedValue(object, path, JsonObject.class);
    }

    @Nonnull
    public static JsonArray getArray(@Nonnull final JsonObject object,
                                     @Nonnull final String path) throws ParsingException {
        return getTypedValue(object, path, JsonArray.class);
    }

    @Nonnull
    public static List<Object> getValues(@Nonnull final JsonArray array,
                                         @Nonnull final String path) throws ParsingException {
        final List<Object> values = new ArrayList<>(array.size());
        for (int index = 0; index < array.size(); index++) {
            values.add(getValue(array.getObject(index), path));
        }
        return values;
    }

    public static JsonArray toJsonArray(final String responseBody) throws ParsingException {
        try {
            return JsonParser.array().from(responseBody);
        } catch (final JsonParserException error) {
            throw new ParsingException("Could not parse JSON", error);
        }
    }

    public static JsonObject toJsonObject(final String responseBody) throws ParsingException {
        try {
            return JsonParser.object().from(responseBody);
        } catch (final JsonParserException error) {
            throw new ParsingException("Could not parse JSON", error);
        }
    }

    public static JsonObject getJsonData(final String html, final String attribute)
            throws JsonParserException, ArrayIndexOutOfBoundsException {
        final Element element = Jsoup.parse(html).getElementsByAttribute(attribute).first();
        return JsonParser.object().from(element == null ? "" : element.attr(attribute));
    }

    @Nonnull
    public static List<String> getStringListFromJsonArray(@Nonnull final JsonArray array) {
        final List<String> values = new ArrayList<>();
        for (final Object value : array) {
            if (value instanceof String) {
                values.add((String) value);
            }
        }
        return values;
    }
}
