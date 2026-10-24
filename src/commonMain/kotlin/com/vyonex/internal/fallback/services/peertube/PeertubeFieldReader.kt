// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

internal object PeertubeFieldReader {
    fun valueAt(item: Map<String, Any?>, path: String): Any? {
        var value: Any? = item
        for (key in path.split('.')) {
            value = (value as? Map<*, *>)?.get(key) ?: return null
        }
        return value
    }

    fun stringAt(item: Map<String, Any?>, path: String): String? =
        valueAt(item, path) as? String

    fun numberAt(item: Map<String, Any?>, path: String): Number? =
        valueAt(item, path) as? Number
}
