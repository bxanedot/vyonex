// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.json;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonLayerTest {

    @Test
    void parsesNestedValuesAndPreservesIntegerPrecision() throws JsonParserException {
        final JsonObject object = JsonParser.object().from(
                "{\"title\":\"caf\\u00e9\\ntrack\",\"large\":9007199254740993,"
                        + "\"enabled\":true,\"empty\":null,\"items\":[{\"id\":7},false]}"
        );

        assertEquals("café\ntrack", object.getString("title"));
        assertEquals(9007199254740993L, object.getLong("large"));
        assertEquals(true, object.getBoolean("enabled"));
        assertTrue(object.has("empty"));
        assertTrue(object.isNull("empty"));
        assertEquals(7, object.getArray("items").getObject(0).getInt("id"));
        assertFalse(object.getArray("items").getBoolean(1));
        assertEquals(new BigDecimal("9007199254740993"), object.getNumber("large"));
    }

    @Test
    void usesDefaultsForMissingAndWrongTypeValues() throws JsonParserException {
        final JsonObject object = JsonParser.object().from("{\"wrong\":{},\"empty\":null}");

        assertEquals("fallback", object.getString("missing", "fallback"));
        assertNull(object.getString("empty"));
        assertEquals(9, object.getInt("wrong", 9));
        assertEquals(0, object.getArray("wrong").size());
        assertEquals(0, object.getObject("missing").size());
        assertFalse(object.has("missing"));
        assertFalse(object.isNull("missing"));
        assertThrows(IllegalStateException.class, () -> JsonObject.builder().end());
    }

    @Test
    void buildsAndSerializesNestedRequestJson() throws JsonParserException {
        final JsonObject request = JsonObject.builder()
                .object("context")
                    .object("client")
                        .value("name", "VyonEx \"client\"")
                        .end()
                    .end()
                .array("flags")
                    .value("line\nbreak")
                    .value(9007199254740993L)
                    .end()
                .done();

        final String json = JsonWriter.string(request);
        final JsonObject reparsed = JsonParser.object().from(json);
        assertEquals("VyonEx \"client\"", reparsed.getObject("context").getObject("client")
                .getString("name"));
        assertEquals("line\nbreak", reparsed.getArray("flags").getString(0));
        assertEquals(9007199254740993L, reparsed.getArray("flags").getLong(1));

        final String writerJson = JsonWriter.string()
                .object()
                    .value("quote", "a\\b\"c")
                    .array("nullable")
                        .nul()
                    .end()
                .end()
                .done();
        final JsonObject writerObject = JsonParser.object().from(writerJson);
        assertEquals("a\\b\"c", writerObject.getString("quote"));
        assertTrue(writerObject.getArray("nullable").isNull(0));
    }

    @Test
    void parsesUtf8InputStreams() throws JsonParserException {
        final JsonArray array = JsonParser.array().from(new ByteArrayInputStream(
                "[\"café\",{\"id\":2}]".getBytes(StandardCharsets.UTF_8)));

        assertEquals("café", array.getString(0));
        assertEquals(2, array.getObject(1).getInt("id"));
    }

    @Test
    void rejectsMalformedAndTrailingJson() {
        assertThrows(JsonParserException.class, () -> JsonParser.object().from("{broken}"));
        assertThrows(JsonParserException.class, () -> JsonParser.array().from("[] {}"));
        assertThrows(JsonParserException.class, () -> JsonParser.array().from(new ByteArrayInputStream(
                "[] {}".getBytes(StandardCharsets.UTF_8))));
    }
}
