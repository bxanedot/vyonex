// bxane
package com.vyonex.internal.primary.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YouTubeUrlParserContractTest {
    @Test
    fun parsesVideoLinksWithOrWithoutScheme() {
        val video = YouTubeUrlParser.ParsedUrl.Video(VIDEO_ID)

        assertEquals(video, YouTubeUrlParser.parse("youtube.com/watch?v=$VIDEO_ID"))
        assertEquals(video, YouTubeUrlParser.parse("https://youtu.be/$VIDEO_ID"))
        assertEquals(video, YouTubeUrlParser.parse("HTTPS://WWW.YOUTUBE.COM/watch?v=$VIDEO_ID"))
        assertEquals(video, YouTubeUrlParser.parse("https://music.youtube.com/shorts/$VIDEO_ID"))
    }

    @Test
    fun distinguishesPlaylistAlbumAndArtistLinks() {
        assertEquals(
            YouTubeUrlParser.ParsedUrl.Playlist("PL123"),
            YouTubeUrlParser.parse("https://www.youtube.com/playlist?list=PL123"),
        )
        assertEquals(
            YouTubeUrlParser.ParsedUrl.Album("OLAK5uy_example"),
            YouTubeUrlParser.parse("https://music.youtube.com/playlist?list=OLAK5uy_example"),
        )
        assertEquals(
            YouTubeUrlParser.ParsedUrl.Album("MPREabc123"),
            YouTubeUrlParser.parse("https://music.youtube.com/browse/MPREabc123"),
        )
        assertEquals(
            YouTubeUrlParser.ParsedUrl.Artist("UC123"),
            YouTubeUrlParser.parse("https://music.youtube.com/channel/UC123"),
        )
    }

    @Test
    fun rejectsUntrustedHostsAndMalformedVideoIds() {
        assertNull(YouTubeUrlParser.parse("https://youtube.com.attacker.test/watch?v=$VIDEO_ID"))
        assertNull(YouTubeUrlParser.parse("https://youtube.com@attacker.test/watch?v=$VIDEO_ID"))
        assertNull(YouTubeUrlParser.parse("https://youtube.com/watch?v=short"))
        assertNull(YouTubeUrlParser.parse("https://youtu.be/$VIDEO_ID/extra"))
    }

    private companion object {
        const val VIDEO_ID = "abcdefghijk"
    }
}
