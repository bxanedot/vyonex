// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.json;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.function.Function;

public final class JsonBuilder<T> {
    private final Deque<Object> stack = new ArrayDeque<>();
    private final Function<Object, T> result;
    private final boolean acceptsRoot;
    private boolean rootStarted;
    private Object root;

    JsonBuilder(final Object root, final Function<Object, T> result) {
        this.root = root;
        this.result = result;
        acceptsRoot = root == null;
        rootStarted = root != null;
        if (rootStarted) stack.push(root);
    }

    public T done() { return result.apply(root); }

    public JsonBuilder<T> array(final Collection<?> values) {
        return addValue(new JsonArray(values));
    }
    public JsonBuilder<T> array(final String key, final Collection<?> values) {
        return addValue(key, new JsonArray(values));
    }
    public JsonBuilder<T> object(final Map<?, ?> values) {
        return addValue(new JsonObject(castMap(values)));
    }
    public JsonBuilder<T> object(final String key, final Map<?, ?> values) {
        return addValue(key, new JsonObject(castMap(values)));
    }

    public JsonBuilder<T> nul() { return addValue(null); }
    public JsonBuilder<T> nul(final String key) { return addValue(key, null); }
    public JsonBuilder<T> value(final Object value) { return addValue(value); }
    public JsonBuilder<T> value(final String key, final Object value) { return addValue(key, value); }
    public JsonBuilder<T> array() { return open(new JsonArray(), null); }
    public JsonBuilder<T> object() { return open(new JsonObject(), null); }
    public JsonBuilder<T> array(final String key) { return open(new JsonArray(), key); }
    public JsonBuilder<T> object(final String key) { return open(new JsonObject(), key); }

    public JsonBuilder<T> end() {
        if (stack.isEmpty()) throw new IllegalStateException("No open JSON value");
        if (stack.size() == 1 && !acceptsRoot) throw new IllegalStateException("Cannot end the root JSON value");
        stack.pop();
        return this;
    }

    private JsonBuilder<T> open(final Object child, final String key) {
        if (stack.isEmpty()) {
            if (!acceptsRoot || rootStarted) throw new IllegalStateException("Root JSON value is already complete");
            root = child;
            rootStarted = true;
            stack.push(child);
        } else {
            addValue(key, child);
            stack.push(child);
        }
        return this;
    }

    private JsonBuilder<T> addValue(final Object value) {
        if (stack.isEmpty()) throw new IllegalStateException("Open a JSON array before adding an unkeyed value");
        final Object parent = stack.peek();
        if (!(parent instanceof JsonArray)) throw new IllegalStateException("Unkeyed value requires a JSON array");
        ((JsonArray) parent).add(value);
        return this;
    }

    private JsonBuilder<T> addValue(final String key, final Object value) {
        if (stack.isEmpty()) throw new IllegalStateException("Open a JSON object before adding a keyed value");
        final Object parent = stack.peek();
        if (!(parent instanceof JsonObject)) throw new IllegalStateException("Keyed value requires a JSON object");
        ((JsonObject) parent).put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    private static Map<? extends String, ?> castMap(final Map<?, ?> map) {
        return (Map<? extends String, ?>) map;
    }
}
