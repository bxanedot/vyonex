// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabInfo;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.playlist.PlaylistInfo;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubePlaylistLinkHandlerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PeertubeProfilesAndListsTest {
    private static final String BASE = "https://tube.example";
    private Downloader previousDownloader;
    private Localization previousLocalization;
    private ContentCountry previousContentCountry;

    @BeforeEach
    void rememberVyonExState() {
        previousDownloader = VyonEx.getDownloader();
        previousLocalization = VyonEx.getPreferredLocalization();
        previousContentCountry = VyonEx.getPreferredContentCountry();
    }

    @AfterEach
    void restoreVyonExState() {
        VyonEx.init(previousDownloader, previousLocalization, previousContentCountry);
    }

    @Test
    void accountIncludesFollowerCountsFromItsChannels() throws Exception {
        final String accountUrl = BASE + "/api/v1/accounts/alice@tube.example";
        final String channelsUrl = accountUrl + "/video-channels";
        final StubDownloader downloader = new StubDownloader(Map.of(
                accountUrl, """
                {"id":7,"displayName":"Alice","description":"Producer","followersCount":5,
                 "avatar":{"path":"/alice.png"},"banner":{"path":"/account-banner.jpg"}}
                """,
                channelsUrl, """
                {"data":[{"followersCount":2},{"followersCount":3}]}
                """));
        VyonEx.init(downloader);
        final PeertubeService service = new PeertubeService(3, new PeertubeInstance(BASE, "Offline"));
        final ListLinkHandler link = service.getChannelLHFactory()
                .fromUrl(BASE + "/a/alice@tube.example");
        final ChannelExtractor account = service.getChannelExtractor(link);
        account.fetchPage();

        assertEquals("Alice", account.getName());
        assertEquals("Producer", account.getDescription());
        assertEquals(10, account.getSubscriberCount());
        assertEquals(BASE + "/feeds/videos.xml?accountId=7", account.getFeedUrl());
        assertEquals(BASE + "/accounts/alice@tube.example/videos",
                account.getTabs().get(0).getUrl());
        assertEquals(BASE + "/accounts/alice@tube.example/video-channels",
                account.getTabs().get(1).getUrl());
        assertEquals(BASE + "/alice.png", account.getAvatars().get(0).getUrl());
        assertEquals(BASE + "/account-banner.jpg", account.getBanners().get(0).getUrl());
    }

    @Test
    void channelExposesItsOwnerAndPlaylistTab() throws Exception {
        final String channelUrl = BASE + "/api/v1/video-channels/music@tube.example";
        final StubDownloader downloader = new StubDownloader(Map.of(channelUrl, """
                {"id":9,"displayName":"Music","description":"Independent music","followersCount":12,
                 "avatar":{"path":"/channel.png"},
                 "ownerAccount":{"name":"alice","url":"https://tube.example/a/alice",
                   "avatar":{"path":"/alice.png"}},"banner":{"path":"/banner.jpg"}}
                """));
        VyonEx.init(downloader);
        final PeertubeService service = new PeertubeService(3, new PeertubeInstance(BASE, "Offline"));
        final ChannelExtractor channel = service.getChannelExtractor(service.getChannelLHFactory()
                .fromUrl(BASE + "/c/music@tube.example"));
        channel.fetchPage();

        assertEquals("Music", channel.getName());
        assertEquals("Independent music", channel.getDescription());
        assertEquals(12, channel.getSubscriberCount());
        assertEquals(BASE + "/channel.png", channel.getAvatars().get(0).getUrl());
        assertEquals("alice", channel.getParentChannelName());
        assertEquals("https://tube.example/a/alice", channel.getParentChannelUrl());
        assertEquals(BASE + "/alice.png", channel.getParentChannelAvatars().get(0).getUrl());
        assertEquals(BASE + "/feeds/videos.xml?videoChannelId=9", channel.getFeedUrl());
        assertEquals(ChannelTabs.PLAYLISTS,
                service.getChannelTabExtractor(channel.getTabs().get(1)).getName());
        assertEquals(BASE + "/banner.jpg", channel.getBanners().get(0).getUrl());
    }

    @Test
    void channelKeepsUnavailableAndMalformedResponseErrors() throws Exception {
        final String channelUrl = BASE + "/api/v1/video-channels/music@tube.example";
        final PeertubeService service = new PeertubeService(3, new PeertubeInstance(BASE, "Offline"));
        final ListLinkHandler link = service.getChannelLHFactory()
                .fromUrl(BASE + "/c/music@tube.example");

        VyonEx.init(new StubDownloader(Map.of(channelUrl, "{\"error\":\"Channel unavailable\"}")));
        final ChannelExtractor unavailable = service.getChannelExtractor(link);
        final ContentNotAvailableException unavailableError = assertThrows(
                ContentNotAvailableException.class, unavailable::fetchPage);
        assertEquals("Channel unavailable", unavailableError.getMessage());

        VyonEx.init(new StubDownloader(Map.of(channelUrl, "not-json")));
        final ChannelExtractor malformed = service.getChannelExtractor(link);
        final ParsingException parseError = assertThrows(ParsingException.class, malformed::fetchPage);
        assertEquals("Invalid PeerTube channel response", parseError.getMessage());
    }

    @Test
    void playlistAndChannelTabReturnPagedVideos() throws Exception {
        final String playlistUrl = BASE + "/api/v1/video-playlists/list-1";
        final String playlistVideosUrl = playlistUrl + "/videos?start=0&count=12";
        final String channelVideosUrl = BASE
                + "/api/v1/video-channels/music@tube.example/videos?start=0&count=12";
        final String videoList = """
                {"total":13,"data":[{"uuid":"video-1","name":"First video","views":4,
                 "account":{"name":"alice","host":"tube.example","displayName":"Alice"}}]}
                """;
        final StubDownloader downloader = new StubDownloader(Map.of(
                playlistUrl, """
                {"displayName":"Mix","description":"Playlist notes","videosLength":1,
                 "thumbnailPath":"/mix.jpg",
                 "ownerAccount":{"displayName":"Alice","url":"https://tube.example/a/alice"},
                 "videoChannel":{"displayName":"Music","url":"https://tube.example/c/music"}}
                """,
                playlistVideosUrl, videoList,
                channelVideosUrl, videoList));
        VyonEx.init(downloader);
        final PeertubeService service = new PeertubeService(3, new PeertubeInstance(BASE, "Offline"));

        final var playlistLink = PeertubePlaylistLinkHandlerFactory.getInstance()
                .fromId("list-1", BASE);
        final var playlist = service.getPlaylistExtractor(playlistLink);
        playlist.fetchPage();
        assertEquals("Mix", playlist.getName());
        assertEquals(1, playlist.getStreamCount());
        assertEquals("Alice", playlist.getUploaderName());
        assertEquals("Music", playlist.getSubChannelName());
        assertEquals(BASE + "/mix.jpg", playlist.getThumbnails().get(0).getUrl());

        final PlaylistInfo playlistInfo = PlaylistInfo.getInfo(playlist);
        assertEquals("Mix", playlistInfo.getName());
        assertEquals(1, playlistInfo.getStreamCount());
        assertEquals("Music", playlistInfo.getSubChannelName());
        assertEquals(1, playlistInfo.getRelatedItems().size());

        final var firstVideoPage = playlist.getInitialPage();
        assertEquals(1, firstVideoPage.getItems().size());
        assertEquals("First video", firstVideoPage.getItems().get(0).getName());
        assertNotNull(firstVideoPage.getNextPage());

        final var tabLink = PeertubeChannelTabLinkHandlerFactory.getInstance().fromQuery(
                "video-channels/music@tube.example", List.of(ChannelTabs.VIDEOS), "", BASE);
        final var tab = service.getChannelTabExtractor(tabLink);
        final var tabPage = tab.getInitialPage();
        assertEquals(1, tabPage.getItems().size());
        assertEquals("First video", tabPage.getItems().get(0).getName());
        assertNotNull(tabPage.getNextPage());

        final ChannelTabInfo tabInfo = ChannelTabInfo.getInfo(tab);
        assertEquals(ChannelTabs.VIDEOS, tabInfo.getName());
        assertEquals(1, tabInfo.getRelatedItems().size());
    }

    @Test
    void rejectsPlaylistMetadataReturnedWithAnHttpFailure() throws Exception {
        final String playlistUrl = BASE + "/api/v1/video-playlists/list-1";
        VyonEx.init(new Downloader() {
            @Override public Response execute(final Request request) {
                assertEquals(playlistUrl, request.url());
                return new Response(503, "Unavailable", Map.of(),
                        "{\"displayName\":\"Mix\"}", request.url());
            }
        });
        final PeertubeService service = new PeertubeService(3, new PeertubeInstance(BASE, "Offline"));
        final var playlist = service.getPlaylistExtractor(PeertubePlaylistLinkHandlerFactory
                .getInstance().fromId("list-1", BASE));

        assertThrows(ExtractionException.class, playlist::fetchPage);
    }

    private static final class StubDownloader extends Downloader {
        private final Map<String, String> bodies;
        private final List<String> requestedUrls = new ArrayList<>();

        private StubDownloader(final Map<String, String> bodies) { this.bodies = bodies; }

        @Override public Response execute(final Request request)
                throws IOException, ReCaptchaException {
            requestedUrls.add(request.url());
            final String body = bodies.get(request.url());
            if (body == null) throw new IOException("No offline response for " + request.url());
            return new Response(200, "OK", Map.of(), body, request.url());
        }
    }
}
