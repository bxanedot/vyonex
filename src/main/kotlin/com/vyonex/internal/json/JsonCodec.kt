// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.json

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray as JsonTreeArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject as JsonTreeObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import java.math.BigDecimal

internal object JsonCodec {
    private val json = Json

    @JvmStatic
    fun parse(source: String): Any? = fromElement(json.parseToJsonElement(source))

    @JvmStatic
    fun stringify(value: Any?): String = json.encodeToString(JsonElement.serializer(), toElement(value))

    private fun fromElement(element: JsonElement): Any? = when (element) {
        JsonNull -> null
        is JsonTreeObject -> JsonObject().apply {
            element.forEach { (key, value) -> put(key, fromElement(value)) }
        }
        is JsonTreeArray -> JsonArray().apply { element.forEach { add(fromElement(it)) } }
        is JsonPrimitive -> when {
            element.isString -> element.content
            element.booleanOrNull != null -> element.booleanOrNull
            else -> BigDecimal(element.content)
        }
    }

    private fun toElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Number -> json.parseToJsonElement(value.toString()).also {
            require(it is JsonPrimitive && !it.isString) { "Not a JSON number: $value" }
        }
        is Map<*, *> -> JsonTreeObject(value.entries.associate { (key, child) ->
            require(key is String) { "JSON object keys must be strings" }
            key to toElement(child)
        })
        is Iterable<*> -> JsonTreeArray(value.map(::toElement))
        is Array<*> -> JsonTreeArray(value.map(::toElement))
        else -> throw IllegalArgumentException("Unsupported JSON value: ${value.javaClass.name}")
    }
}
