// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.json;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonObject extends LinkedHashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    public JsonObject() { }

    public JsonObject(final Map<? extends String, ?> values) { super(values); }

    public JsonObject(final int initialCapacity) { super(initialCapacity); }

    public JsonObject(final int initialCapacity, final float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public static JsonBuilder<JsonObject> builder() {
        return new JsonBuilder<>(new JsonObject(), JsonObject.class::cast);
    }

    public JsonArray getArray(final String key) { return getArray(key, new JsonArray()); }
    public JsonArray getArray(final String key, final JsonArray fallback) {
        final Object value = get(key);
        return value instanceof JsonArray ? (JsonArray) value : fallback;
    }

    public boolean getBoolean(final String key) { return getBoolean(key, false); }
    public boolean getBoolean(final String key, final Boolean fallback) {
        final Object value = get(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    public double getDouble(final String key) { return getDouble(key, 0); }
    public double getDouble(final String key, final double fallback) {
        final Object value = get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    public float getFloat(final String key) { return getFloat(key, 0); }
    public float getFloat(final String key, final float fallback) {
        final Object value = get(key);
        return value instanceof Number ? ((Number) value).floatValue() : fallback;
    }

    public int getInt(final String key) { return getInt(key, 0); }
    public int getInt(final String key, final int fallback) {
        final Object value = get(key);
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    public long getLong(final String key) { return getLong(key, 0); }
    public long getLong(final String key, final long fallback) {
        final Object value = get(key);
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    public Number getNumber(final String key) { return getNumber(key, null); }
    public Number getNumber(final String key, final Number fallback) {
        final Object value = get(key);
        return value instanceof Number ? (Number) value : fallback;
    }

    public JsonObject getObject(final String key) { return getObject(key, new JsonObject()); }
    public JsonObject getObject(final String key, final JsonObject fallback) {
        final Object value = get(key);
        return value instanceof JsonObject ? (JsonObject) value : fallback;
    }

    public String getString(final String key) { return getString(key, null); }
    public String getString(final String key, final String fallback) {
        final Object value = get(key);
        return value instanceof String ? (String) value : fallback;
    }

    public boolean has(final String key) { return containsKey(key); }
    public boolean isBoolean(final String key) { return get(key) instanceof Boolean; }
    public boolean isNull(final String key) { return containsKey(key) && get(key) == null; }
    public boolean isNumber(final String key) { return get(key) instanceof Number; }
    public boolean isString(final String key) { return get(key) instanceof String; }
}
