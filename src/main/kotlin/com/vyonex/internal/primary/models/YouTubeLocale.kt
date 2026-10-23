// bxane
package com.vyonex.internal.primary.models

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeLocale(
    val gl: String,
    val hl: String,
) {
    internal fun acceptLanguageHeader(): String {
        val languageTag = hl.trim().replace('_', '-')
        val language = languageTag.substringBefore('-')
        if (language.isEmpty()) return ""

        val preferred = if ('-' in languageTag || gl.isEmpty()) languageTag else "$languageTag-$gl"
        return if (preferred == language) language else "$preferred,$language;q=0.9"
    }
}
