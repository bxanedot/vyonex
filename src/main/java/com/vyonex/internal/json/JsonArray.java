// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

public class JsonArray extends ArrayList<Object> {
    private static final long serialVersionUID = 1L;

    public JsonArray() { }
    public JsonArray(final int initialCapacity) { super(initialCapacity); }
    public JsonArray(final Collection<?> values) { super(values); }

    public static JsonArray from(final Object... values) { return new JsonArray(Arrays.asList(values)); }
    public static JsonBuilder<JsonArray> builder() {
        return new JsonBuilder<>(new JsonArray(), JsonArray.class::cast);
    }

    @Override
    public Object get(final int index) { return index >= 0 && index < size() ? super.get(index) : null; }

    public JsonArray getArray(final int index) { return getArray(index, new JsonArray()); }
    public JsonArray getArray(final int index, final JsonArray fallback) {
        final Object value = get(index);
        return value instanceof JsonArray ? (JsonArray) value : fallback;
    }

    public boolean getBoolean(final int index) { return getBoolean(index, false); }
    public boolean getBoolean(final int index, final Boolean fallback) {
        final Object value = get(index);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    public double getDouble(final int index) { return getDouble(index, 0); }
    public double getDouble(final int index, final double fallback) {
        final Object value = get(index);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    public float getFloat(final int index) { return getFloat(index, 0); }
    public float getFloat(final int index, final float fallback) {
        final Object value = get(index);
        return value instanceof Number ? ((Number) value).floatValue() : fallback;
    }

    public int getInt(final int index) { return getInt(index, 0); }
    public int getInt(final int index, final int fallback) {
        final Object value = get(index);
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    public long getLong(final int index) { return getLong(index, 0); }
    public long getLong(final int index, final long fallback) {
        final Object value = get(index);
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    public Number getNumber(final int index) { return getNumber(index, null); }
    public Number getNumber(final int index, final Number fallback) {
        final Object value = get(index);
        return value instanceof Number ? (Number) value : fallback;
    }

    public JsonObject getObject(final int index) { return getObject(index, new JsonObject()); }
    public JsonObject getObject(final int index, final JsonObject fallback) {
        final Object value = get(index);
        return value instanceof JsonObject ? (JsonObject) value : fallback;
    }

    public String getString(final int index) { return getString(index, null); }
    public String getString(final int index, final String fallback) {
        final Object value = get(index);
        return value instanceof String ? (String) value : fallback;
    }

    public boolean has(final int index) { return index >= 0 && index < size(); }
    public boolean isBoolean(final int index) { return get(index) instanceof Boolean; }
    public boolean isNull(final int index) { return has(index) && get(index) == null; }
    public boolean isNumber(final int index) { return get(index) instanceof Number; }
    public boolean isString(final int index) { return get(index) instanceof String; }
    public <T> Stream<T> streamAs(final Class<T> type) {
        return stream().filter(type::isInstance).map(type::cast);
    }
    public Stream<JsonObject> streamAsJsonObjects() { return streamAs(JsonObject.class); }
}
