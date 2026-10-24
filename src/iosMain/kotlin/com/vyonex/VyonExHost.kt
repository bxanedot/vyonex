// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex

import com.vyonex.internal.primary.extraction.TokenProvider

/** iOS host services; VyonEx owns its Darwin HTTP transport. */
interface VyonExHost : VyonExHostServices {
    val tokenProvider: TokenProvider?
}
