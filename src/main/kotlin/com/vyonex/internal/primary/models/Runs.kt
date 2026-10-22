// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.models

import kotlinx.serialization.Serializable

private val RUN_SEPARATOR_REGEX = Regex("[&,]")

@Serializable
data class Runs(val runs: List<Run>? = null)

@Serializable
data class Run(
    val text: String,
    val navigationEndpoint: NavigationEndpoint? = null,
    val bold: Boolean? = null,
    val italics: Boolean? = null,
)

@Serializable
data class NavigationEndpoint(
    val browseEndpoint: BrowseEndpoint? = null,
    val watchEndpoint: WatchEndpoint? = null,
    val watchPlaylistEndpoint: WatchPlaylistEndpoint? = null,
)

@Serializable
data class BrowseEndpoint(
    val browseId: String,
    val params: String? = null,
)

@Serializable
data class WatchEndpoint(
    val videoId: String,
    val playlistId: String? = null,
    val index: Int? = null,
    val params: String? = null,
)

@Serializable
data class WatchPlaylistEndpoint(
    val playlistId: String,
    val params: String? = null,
)

fun List<Run>.splitBySeparator(): List<List<Run>> {
    val result = mutableListOf<List<Run>>()
    var group = mutableListOf<Run>()
    for (run in this) {
        if (run.text == " • ") {
            result.add(group)
            group = mutableListOf()
        } else {
            group.add(run)
        }
    }
    result.add(group)
    return result
}

fun List<List<Run>>.clean(): List<List<Run>> {
    val first = firstOrNull()?.firstOrNull()
    return if (first?.navigationEndpoint != null || first?.text?.contains(RUN_SEPARATOR_REGEX) == true) {
        this
    } else {
        drop(1)
    }
}

fun List<Run>.oddElements(): List<Run> = filterIndexed { index, _ -> index % 2 == 0 }
