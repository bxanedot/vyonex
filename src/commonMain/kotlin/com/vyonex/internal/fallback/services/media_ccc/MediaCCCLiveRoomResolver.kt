// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc

internal data class MediaCCCLiveRoom(
    val display: String?,
    val conferenceName: String?,
    val conferenceSlug: String,
    val conferenceDescription: String?,
    val group: String,
    val thumbnails: List<MediaCCCLiveThumbnail>,
    val streams: List<MediaCCCLiveStream>,
) {
    fun manifest(format: String): String = streams.asSequence()
        .flatMap { it.formats.asSequence() }
        .firstNotNullOfOrNull { source ->
            source.url?.takeIf { source.format == format && it.isNotEmpty() }
        }
        .orEmpty()
}

internal data class MediaCCCLiveThumbnail(
    val url: String,
    val quality: Quality,
) {
    enum class Quality { MEDIUM, HIGH }
}

internal data class MediaCCCLiveStream(
    val type: String,
    val width: Int,
    val height: Int,
    val formats: List<MediaCCCLiveSourcePayload>,
)

internal object MediaCCCLiveRoomResolver {
    fun findRoom(
        conferences: List<MediaCCCLiveConferencePayload>,
        target: String,
    ): MediaCCCLiveRoom? {
        for (conference in conferences) {
            val slug = conference.slug ?: continue
            for (group in conference.groups.orEmpty()) {
                for (room in group.rooms.orEmpty()) {
                    if (room.slug == null || "$slug/${room.slug}" != target) continue
                    return MediaCCCLiveRoom(
                        display = room.display,
                        conferenceName = conference.conference,
                        conferenceSlug = slug,
                        conferenceDescription = conference.description,
                        group = group.group.orEmpty(),
                        thumbnails = listOfNotNull(
                            room.thumb?.takeIf(String::isNotEmpty)?.let {
                                MediaCCCLiveThumbnail(it, MediaCCCLiveThumbnail.Quality.MEDIUM)
                            },
                            room.poster?.takeIf(String::isNotEmpty)?.let {
                                MediaCCCLiveThumbnail(it, MediaCCCLiveThumbnail.Quality.HIGH)
                            },
                        ),
                        streams = room.streams.orEmpty().map { stream ->
                            MediaCCCLiveStream(
                                type = stream.type.orEmpty(),
                                width = stream.videoSize?.getOrNull(0) ?: 0,
                                height = stream.videoSize?.getOrNull(1) ?: 0,
                                formats = stream.sources(),
                            )
                        },
                    )
                }
            }
        }
        return null
    }
}
