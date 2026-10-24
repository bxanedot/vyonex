// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube.linkHandler

import com.vyonex.internal.fallback.exceptions.ParsingException
import java.net.URI
import java.net.URISyntaxException

internal class PeertubeUrl private constructor() {
    public companion object {
        @JvmStatic
        @Throws(ParsingException::class)
        public fun parse(value: String): URI {
            try {
                val parsed = URI(value)
                val scheme = parsed.scheme
                if (parsed.host != null && parsed.userInfo == null
                    && (scheme.equals("http", ignoreCase = true)
                        || scheme.equals("https", ignoreCase = true))
                ) return parsed
            } catch (_: URISyntaxException) {
                // Convert URI syntax failures into the service's parsing error below.
            } catch (_: NullPointerException) {
                // Keep the same error type for a missing URL.
            }
            throw ParsingException("Invalid PeerTube URL: $value")
        }

        @JvmStatic
        @Throws(ParsingException::class)
        public fun segments(value: String): List<String> =
            parse(value).rawPath?.split('/')?.filter(String::isNotEmpty) ?: emptyList()

        @JvmStatic
        public fun accepts(value: String): Boolean = try {
            parse(value)
            true
        } catch (_: ParsingException) {
            false
        }
    }
}
