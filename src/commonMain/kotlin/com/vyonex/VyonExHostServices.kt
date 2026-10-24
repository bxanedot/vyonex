// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex

/** Platform-neutral services consumed by VyonEx. */
interface VyonExHostServices {
    suspend fun session(): VyonExSession

    val configStore: VyonExConfigStore

    fun nowMs(): Long

    fun log(level: VyonExLogLevel, message: String, trackId: String? = null)
}
