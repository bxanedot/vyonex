// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.bandcamp.extractors

import com.vyonex.internal.fallback.VyonEx
import com.vyonex.internal.fallback.downloader.Downloader
import com.vyonex.internal.fallback.downloader.Request
import com.vyonex.internal.fallback.downloader.Response
import com.vyonex.internal.fallback.exceptions.ExtractionException
import com.vyonex.internal.fallback.stream.StreamType
import com.vyonex.internal.fallback.services.bandcamp.BandcampService
import com.vyonex.internal.fallback.services.bandcamp.linkHandler.BandcampSearchQueryHandlerFactory
import com.vyonex.internal.fallback.services.bandcamp.extractors.streaminfoitem.BandcampSearchStreamInfoItemExtractor
import org.jsoup.Jsoup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BandcampSearchPageParserTest {
    @Test
    fun mapsCurrentSearchCardsAndSkipsUnsupportedKinds() {
        val parsed = BandcampSearchPageParser.parse(
            """<li class="searchresult data-search"><div class="art"><img src="https://img.test/artist.jpg"></div>
                <div class="result-info"><div class="itemtype">ARTIST</div><div class="heading">Artist</div>
                  <div class="itemurl"><a href="https://artist.bandcamp.com/">https://artist.bandcamp.com/</a></div>
                  <div class="subhead">Electronic</div></div></li>
                <li class="searchresult"><div class="art"><img src="https://img.test/album.jpg"></div>
                <div class="result-info"><div class="itemtype">Album</div><div class="heading">Record</div>
                  <div class="itemurl">https://artist.bandcamp.com/album/record</div>
                  <div class="subhead">Record by Artist</div><div class="length">12 tracks</div></div></li>
                <li class="searchresult"><div class="art"><img src="https://img.test/track.jpg"></div>
                <div class="result-info"><div class="itemtype">Track</div><div class="heading">Song</div>
                  <div class="itemurl">https://artist.bandcamp.com/track/song</div>
                  <div class="subhead">Song by Artist</div></div></li>
                <li class="searchresult"><div class="result-info"><div class="itemtype">LABEL</div>
                  <div class="heading">Ignored</div></div></li>
                <div class="pagelist"><ul><li><span class="chosen">1</span></li>
                  <li><a href="?q=ambient&amp;page=2">2</a></li></ul></div>""",
            "https://bandcamp.com/search?q=ambient&page=1",
        )

        assertEquals(
            listOf(
                BandcampSearchItem.Artist(
                    "Artist",
                    "https://artist.bandcamp.com/",
                    "Electronic",
                    "https://img.test/artist.jpg",
                ),
                BandcampSearchItem.Album(
                    "Record",
                    "https://artist.bandcamp.com/album/record",
                    "Artist",
                    12,
                    "https://img.test/album.jpg",
                ),
                BandcampSearchItem.Track(
                    "Song",
                    "https://artist.bandcamp.com/track/song",
                    "Artist",
                    "https://img.test/track.jpg",
                ),
            ),
            parsed.items,
        )
        assertEquals("https://bandcamp.com/search?q=ambient&page=2", parsed.nextPageUrl)
    }

    @Test
    fun searchAdaptersKeepTheirResultKindsAndPagination() {
        val html = """<div class="searchresult"><div class="result-info">
            <div class="itemtype">Artist</div><div class="heading">Artist</div>
            <div class="itemurl">https://artist.bandcamp.com/</div></div></div>
            <div class="searchresult"><div class="result-info">
            <div class="itemtype">Album</div><div class="heading">Album</div>
            <div class="itemurl">https://artist.bandcamp.com/album/album</div>
            <div class="length">8 tracks</div></div></div>
            <div class="searchresult"><div class="result-info">
            <div class="itemtype">Track</div><div class="heading">Track</div>
            <div class="itemurl">https://artist.bandcamp.com/track/track</div></div></div>
            <div class="pagelist"><a rel="next" href="?q=ambient&amp;page=2">Next</a></div>"""
        val previous = VyonEx.getDownloader()
        val expectedUrl = "https://bandcamp.com/search?q=ambient&page=1"
        VyonEx.init(
            object : Downloader() {
                override fun execute(request: Request): Response {
                    assertEquals(expectedUrl, request.url())
                    assertEquals(listOf("identity"), request.headers()["Cookie"])
                    return Response(200, "OK", emptyMap(), html, request.url())
                }
            },
        )
        try {
            val service = BandcampService(1)
            val handler = BandcampSearchQueryHandlerFactory.getInstance()
                .fromQuery("ambient", emptyList(), "")
            val page = service.getSearchExtractor(handler).getInitialPage()

            assertEquals(listOf("Artist", "Album", "Track"), page.getItems().map { it.getName() })
            assertEquals(listOf("CHANNEL", "PLAYLIST", "STREAM"), page.getItems().map { it.getInfoType().name })
            assertTrue(page.hasNextPage())
            assertEquals("https://bandcamp.com/search?q=ambient&page=2", page.getNextPage()?.getUrl())
        } finally {
            VyonEx.init(previous)
        }
    }

    @Test
    fun searchAdaptersPreserveBandcampMetadata() {
        val artist = BandcampChannelInfoItemExtractor(
            BandcampSearchItem.Artist("Artist", "https://artist.bandcamp.com/", "Electronic", "https://img.test/artist.jpg"),
        )
        assertEquals("Artist", artist.getName())
        assertEquals("https://artist.bandcamp.com/", artist.getUrl())
        assertEquals("Electronic", artist.getDescription())
        assertEquals(-1, artist.getSubscriberCount())

        val album = BandcampPlaylistInfoItemExtractor(
            BandcampSearchItem.Album(
                "Record", "https://artist.bandcamp.com/album/record", "Artist", 12, "https://img.test/album.jpg",
            ),
        )
        assertEquals("Record", album.getName())
        assertEquals("https://artist.bandcamp.com/album/record", album.getUrl())
        assertEquals("Artist", album.getUploaderName())
        assertEquals(12, album.getStreamCount())

        val track = BandcampSearchStreamInfoItemExtractor(
            BandcampSearchItem.Track("Song", "https://artist.bandcamp.com/track/song", "Artist", "https://img.test/song.jpg"),
            null,
        )
        assertEquals("Song", track.getName())
        assertEquals("https://artist.bandcamp.com/track/song", track.getUrl())
        assertEquals("Artist", track.getUploaderName())
        assertEquals("https://img.test/song.jpg", track.getThumbnails().single().getUrl())
        assertEquals(StreamType.AUDIO_STREAM, track.getStreamType())
        assertEquals(-1, track.getViewCount())
        assertFalse(track.isUploaderVerified())
    }

    @Test
    fun botCheckPagesFailInsteadOfLookingLikeAnEmptySearch() {
        assertFailsWith<ExtractionException> {
            BandcampSearchPageParser.parse(
                "<html><body><h1>Verify You Are Human</h1></body></html>",
                "https://bandcamp.com/search?q=ambient&page=1",
            )
        }
        assertFailsWith<ExtractionException> {
            BandcampSearchPageParser.parse(
                "<form id='captcha'></form>",
                "https://bandcamp.com/search?q=ambient&page=1",
            )
        }
    }

    @Test
    fun challengeWordsInsideARealResultDoNotHideTheResult() {
        val parsed = BandcampSearchPageParser.parse(
            """<div class="searchresult"><div class="result-info">
                <div class="itemtype">ARTIST</div><div class="heading">Access Denied Captcha</div>
                <div class="itemurl">https://artist.bandcamp.com/</div>
                <div class="subhead">Verify You Are Human</div></div></div>""",
            "https://bandcamp.com/search?q=test&page=1",
        )

        assertEquals(1, parsed.items.size)
        assertEquals("Access Denied Captcha", (parsed.items.single() as BandcampSearchItem.Artist).name)
    }

    @Test
    fun resolvesRelativePaginationAndRejectsMissingOrUnsafeLinks() {
        val base = "https://bandcamp.com/search?q=ambient&page=1"
        assertEquals(
            "https://bandcamp.com/search?q=ambient&page=2",
            BandcampSearchExtractor.findNextPage(
                Jsoup.parse("<div class='pagelist'><ul><li class='selected'><span>1</span></li>"
                    + "<li><a href='?q=ambient&amp;page=2'>2</a></li></ul></div>"),
                base,
            )?.url,
        )
        assertNull(BandcampSearchExtractor.findNextPage(Jsoup.parse("<div>empty</div>"), base))
        assertNull(BandcampSearchExtractor.findNextPage(
            Jsoup.parse("<div class='pagelist'><a class='next' href='https://elsewhere.test/next'>Next</a></div>"),
            base,
        ))
        assertNull(BandcampSearchExtractor.findNextPage(
            Jsoup.parse("<div class='pagelist'><a rel='next' href='#top'>Next</a></div>"),
            base,
        ))
        assertNull(BandcampSearchExtractor.findNextPage(
            Jsoup.parse("<div class='pagelist'><a rel='next' href='https://bandcamp.com:8443/search?page=2'>Next</a></div>"),
            base,
        ))
    }
}
