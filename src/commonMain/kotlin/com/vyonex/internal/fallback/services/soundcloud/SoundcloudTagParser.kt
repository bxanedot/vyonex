// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.soundcloud

internal object SoundcloudTagParser {
    fun parse(input: String?): List<String> {
        if (input.isNullOrBlank()) return emptyList()
        val tags = mutableListOf<String>()
        val tag = StringBuilder()
        var quoted = false
        var index = 0
        while (index < input.length) {
            val character = input[index]
            if (character == '\\' && index + 1 < input.length
                && (input[index + 1] == '"' || input[index + 1] == '\\')
            ) {
                tag.append(input[++index])
            } else if (character == '"') {
                quoted = !quoted
            } else if (character.isWhitespace() && !quoted) {
                if (tag.isNotEmpty()) {
                    tags.add(tag.toString())
                    tag.clear()
                }
            } else {
                tag.append(character)
            }
            index++
        }
        if (tag.isNotEmpty()) tags.add(tag.toString())
        return tags
    }
}
