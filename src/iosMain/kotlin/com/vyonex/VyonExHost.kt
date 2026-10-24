// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex

import com.vyonex.internal.primary.extraction.TokenProvider
import io.ktor.client.HttpClient

/** iOS host services, with transport and script storage supplied by the app. */
interface VyonExHost : VyonExHostServices {
    val tokenProvider: TokenProvider?
    val filesDir: String?
    val httpClient: HttpClient
}
