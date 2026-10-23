// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonUtilsContractTest {
    @Test
    void readsNestedValuesWithExactTypes() throws JsonParserException, ParsingException {
        final JsonObject root = JsonParser.object().from(
                "{\"item\":{\"name\":\"track\",\"enabled\":true,\"count\":7},"
                        + "\"items\":[{\"id\":1},{\"id\":2}]}"
        );

        assertEquals("track", JsonUtils.getString(root, "item.name"));
        assertEquals(true, JsonUtils.getBoolean(root, "item.enabled"));
        assertEquals(7, JsonUtils.getNumber(root, "item.count").intValue());
        assertEquals(List.of(1, 2), JsonUtils.getValues(root.getArray("items"), "id")
                .stream().map(value -> ((Number) value).intValue()).toList());
        assertThrows(ParsingException.class, () -> JsonUtils.getString(root, "item.count"));
        assertThrows(ParsingException.class, () -> JsonUtils.getValue(root, "missing.value"));
    }

    @Test
    void parsesJsonAndHtmlAttributeValues() throws Exception {
        final JsonArray array = JsonUtils.toJsonArray("[\"first\",2,null,\"last\"]");
        assertEquals(List.of("first", "last"), JsonUtils.getStringListFromJsonArray(array));
        assertEquals("café", JsonUtils.getJsonData(
                "<div data-record=\"{&quot;title&quot;:&quot;café&quot;}\"></div>",
                "data-record").getString("title"));
        assertThrows(ParsingException.class, () -> JsonUtils.toJsonObject("{broken}"));
    }
}
