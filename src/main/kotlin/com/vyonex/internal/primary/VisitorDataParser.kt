// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

internal object VisitorDataParser {
    private const val MAX_VALUE_LENGTH = 4096
    private val homepageValue = Regex(""""(?:VISITOR_DATA|visitorData)"\s*:\s*"([^"]+)"""")

    fun fromServiceWorker(response: String): String? =
        runCatching {
            Json.parseToJsonElement(response.substringAfter('\n').trimStart())
                .jsonArray[0]
                .jsonArray[2]
                .jsonArray[0]
                .jsonArray[0]
                .jsonArray[13]
                .jsonPrimitive
                .contentOrNull
                .validated()
        }.getOrNull()

    fun fromHomepage(html: String): String? =
        homepageValue.find(html)?.groupValues?.get(1).validated()

    private fun String?.validated(): String? =
        this?.takeIf { value ->
            value.isNotBlank() && value.length <= MAX_VALUE_LENGTH &&
                value.all { it.code in 0x21..0x7e }
        }
}
