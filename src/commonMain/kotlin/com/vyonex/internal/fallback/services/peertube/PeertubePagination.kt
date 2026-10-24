// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

internal object PeertubePagination {
    const val START_KEY = "start"
    const val ITEMS_PER_PAGE = 12

    fun nextPageUrl(previousUrl: String, total: Long): String? {
        val queryStart = previousUrl.indexOf('?')
        if (queryStart < 0) return null
        val queryEnd = previousUrl.indexOf('#', queryStart).let {
            if (it < 0) previousUrl.length else it
        }

        var start = queryStart + 1
        while (start < queryEnd) {
            val separator = previousUrl.indexOf('&', start).let {
                if (it < 0 || it > queryEnd) queryEnd else it
            }
            val equals = previousUrl.indexOf('=', start).takeIf { it in start until separator }
            if (previousUrl.substring(start, equals ?: separator) == START_KEY) {
                val valueStart = equals?.plus(1) ?: return null
                val current = decodeOffset(previousUrl.substring(valueStart, separator)) ?: return null
                if (current < 0 || current > Long.MAX_VALUE - ITEMS_PER_PAGE) return null
                val next = current + ITEMS_PER_PAGE
                if (next >= total) return null
                return previousUrl.substring(0, valueStart) + next + previousUrl.substring(separator)
            }
            start = separator + 1
        }
        return null
    }

    private fun decodeOffset(value: String): Long? {
        val decoded = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            when (val character = value[index]) {
                '+' -> decoded.append(' ')
                '%' -> {
                    if (index + 2 >= value.length) return null
                    val high = value[index + 1].hexValue()
                    val low = value[index + 2].hexValue()
                    if (high < 0 || low < 0) return null
                    val byte = high * 16 + low
                    if (byte >= 128) return null
                    decoded.append(byte.toChar())
                    index += 2
                }
                else -> decoded.append(character)
            }
            index++
        }
        return decoded.toString().toLongOrNull()
    }

    private fun Char.hexValue(): Int = when (this) {
        in '0'..'9' -> code - '0'.code
        in 'a'..'f' -> code - 'a'.code + 10
        in 'A'..'F' -> code - 'A'.code + 10
        else -> -1
    }
}
