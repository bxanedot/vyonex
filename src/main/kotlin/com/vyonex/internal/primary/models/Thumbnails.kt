// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.models

import kotlinx.serialization.Serializable

@Serializable
data class Thumbnails(val thumbnails: List<Thumbnail>)

@Serializable
data class Thumbnail(
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
)
