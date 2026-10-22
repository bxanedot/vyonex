// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class JsonParser {
    private JsonParser() { }

    public static JsonParserContext<JsonObject> object() { return new JsonParserContext<>(JsonObject.class); }
    public static JsonParserContext<JsonArray> array() { return new JsonParserContext<>(JsonArray.class); }
    public static JsonParserContext<Object> any() { return new JsonParserContext<>(Object.class); }

    public static final class JsonParserContext<T> {
        private final Class<T> type;
        private JsonParserContext(final Class<T> type) { this.type = type; }

        public T from(final String source) throws JsonParserException {
            try {
                final Object value = JsonCodec.parse(source);
                if (type != Object.class && !type.isInstance(value)) {
                    throw new JsonParserException("JSON value is not a " + type.getSimpleName());
                }
                return type.cast(value);
            } catch (JsonParserException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new JsonParserException("Invalid JSON", e);
            }
        }

        public T from(final Reader reader) throws JsonParserException {
            final StringBuilder source = new StringBuilder();
            final char[] buffer = new char[4096];
            try {
                int count;
                while ((count = reader.read(buffer)) != -1) source.append(buffer, 0, count);
            } catch (IOException e) {
                throw new JsonParserException("Unable to read JSON", e);
            }
            return from(source.toString());
        }

        public T from(final InputStream input) throws JsonParserException {
            try {
                return from(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new JsonParserException("Unable to read JSON", e);
            }
        }

        public T from(final URL url) throws JsonParserException {
            try (InputStream input = url.openStream()) {
                return from(input);
            } catch (IOException e) {
                throw new JsonParserException("Unable to open JSON URL", e);
            }
        }
    }
}
