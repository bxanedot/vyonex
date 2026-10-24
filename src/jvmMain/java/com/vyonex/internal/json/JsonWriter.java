// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.json;

public final class JsonWriter {
    private JsonWriter() { }

    public static JsonBuilder<String> string() {
        return new JsonBuilder<>(null, JsonCodec::stringify);
    }

    public static String string(final Object value) { return JsonCodec.stringify(value); }
}
