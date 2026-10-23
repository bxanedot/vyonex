// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.MultiInfoItemsCollector;
import com.vyonex.internal.fallback.channel.ChannelInfoItem;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.playlist.PlaylistInfoItem;
import com.vyonex.internal.fallback.services.peertube.extractors.PeertubeSepiaStreamInfoItemExtractor;
import com.vyonex.internal.fallback.stream.StreamInfoItem;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

class PeertubeParsingHelperTest {
    @Test
    void advancesPagingWithoutChangingOtherQueryParameters() {
        assertEquals("https://tube.example/api/videos?start=24&count=12&sort=-publishedAt",
                PeertubeParsingHelper.getNextPage(
                        "https://tube.example/api/videos?start=12&count=12&sort=-publishedAt", 30)
                        .getUrl());
        assertNull(PeertubeParsingHelper.getNextPage(
                "https://tube.example/api/videos?start=12&count=12", 24));
        assertNull(PeertubeParsingHelper.getNextPage("https://tube.example/api/videos?count=12", 40));
    }

    @Test
    void advancesPercentEncodedStartAndKeepsUrlSuffixes() {
        assertEquals("https://tube.example/api/videos?filter=a%26b&start=24&count=12#page",
                PeertubeParsingHelper.getNextPage(
                        "https://tube.example/api/videos?filter=a%26b&start=%31%32&count=12#page",
                        30).getUrl());
    }

    @Test
    void reportsUnavailableResponseErrors() throws Exception {
        assertThrows(ContentNotAvailableException.class, () -> PeertubeParsingHelper.validate(
                JsonParser.object().from("{\"error\":\"Video unavailable\"}")));
        PeertubeParsingHelper.validate(JsonParser.object().from("{\"data\":[]}"));
    }

    @Test
    void parsesOnlySuccessfulNonemptyJsonResponses() throws Exception {
        assertEquals("ok", PeertubeParsingHelper.readApiObject(
                new Response(200, "OK", Map.of(), "{\"status\":\"ok\"}", null),
                "search response").getString("status"));
        assertThrows(ExtractionException.class, () -> PeertubeParsingHelper.readApiObject(
                new Response(503, "Unavailable", Map.of(), "{\"status\":\"ok\"}", null),
                "search response"));
        assertThrows(ExtractionException.class, () -> PeertubeParsingHelper.readApiObject(
                new Response(200, "OK", Map.of(), "", null), "search response"));
    }

    @Test
    void readsRequiredAndOptionalNestedFields() throws Exception {
        final var item = JsonParser.object().from("""
                {"account":{"displayName":"Creator","followers":12}}
                """);

        assertEquals("Creator", PeertubeParsingHelper.requiredString(item, "account.displayName"));
        assertEquals(12, PeertubeParsingHelper.requiredNumber(item, "account.followers").intValue());
        assertNull(PeertubeParsingHelper.optionalString(item, "account.missing"));
        assertThrows(ParsingException.class,
                () -> PeertubeParsingHelper.requiredString(item, "account.missing"));
    }

    @Test
    void combinesRemoteThumbnailAndAvatarPaths() throws Exception {
        final var item = JsonParser.object().from("""
                {"thumbnailPath":"/thumb.jpg","previewPath":"/preview.jpg",
                 "avatars":[{"path":"/avatar.jpg","width":96}]}
                """);

        final var thumbnails = PeertubeParsingHelper
                .getThumbnailsFromPlaylistOrVideoItem("https://tube.example", item);
        final var avatars = PeertubeParsingHelper
                .getAvatarsFromOwnerAccountOrVideoChannelObject("https://tube.example", item);

        assertEquals("https://tube.example/thumb.jpg", thumbnails.get(0).getUrl());
        assertEquals(Image.ResolutionLevel.MEDIUM,
                thumbnails.get(1).getEstimatedResolutionLevel());
        assertEquals("https://tube.example/avatar.jpg", avatars.get(0).getUrl());
        assertEquals(96, avatars.get(0).getWidth());
    }

    @Test
    void collectsVideoChannelPlaylistAndSepiaResults() throws Exception {
        final String response = """
                {"data":[
                  {"uuid":"video-1","name":"Video","views":10,"duration":90,
                   "account":{"name":"alice","host":"tube.example","displayName":"Alice",
                     "avatar":{"path":"/alice.jpg"}},
                   "publishedAt":"2026-01-02T03:04:05Z","thumbnailPath":"/video.jpg"},
                  {"displayName":"Channel","url":"https://tube.example/video-channels/c1",
                   "followersCount":7,"description":"Talks","avatars":[{"path":"/channel.jpg"}]},
                  {"displayName":"Playlist","url":"https://tube.example/videos/watch/playlist",
                   "videosLength":2,"thumbnailPath":"/playlist.jpg",
                   "uploader":{"displayName":"Alice","url":"https://tube.example/a/alice"}}
                ]}
                """;
        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(3);

        PeertubeParsingHelper.collectItemsFrom(collector, JsonParser.object().from(response),
                "https://tube.example", false);

        assertEquals(3, collector.getItems().size());
        assertEquals(InfoItem.InfoType.STREAM, collector.getItems().get(0).getInfoType());
        final StreamInfoItem video = (StreamInfoItem) collector.getItems().get(0);
        assertEquals("Video", video.getName());
        assertEquals("https://tube.example/videos/watch/video-1", collector.getItems().get(0).getUrl());
        assertEquals("Alice", video.getUploaderName());
        assertEquals("https://tube.example/accounts/alice@tube.example", video.getUploaderUrl());
        assertEquals("https://tube.example/video.jpg", video.getThumbnails().get(0).getUrl());
        assertEquals("https://tube.example/alice.jpg", video.getUploaderAvatars().get(0).getUrl());
        assertEquals(10, video.getViewCount());
        assertEquals(90, video.getDuration());
        assertEquals(InfoItem.InfoType.CHANNEL, collector.getItems().get(1).getInfoType());
        final ChannelInfoItem channel = (ChannelInfoItem) collector.getItems().get(1);
        assertEquals("Channel", channel.getName());
        assertEquals("https://tube.example/video-channels/c1", channel.getUrl());
        assertEquals("Talks", channel.getDescription());
        assertEquals(7, channel.getSubscriberCount());
        assertEquals("https://tube.example/channel.jpg", channel.getThumbnails().get(0).getUrl());
        assertEquals(InfoItem.InfoType.PLAYLIST, collector.getItems().get(2).getInfoType());
        final PlaylistInfoItem playlist = (PlaylistInfoItem) collector.getItems().get(2);
        assertEquals("Playlist", playlist.getName());
        assertEquals("https://tube.example/videos/watch/playlist", playlist.getUrl());
        assertEquals("Alice", playlist.getUploaderName());
        assertEquals("https://tube.example/a/alice", playlist.getUploaderUrl());
        assertEquals(2, playlist.getStreamCount());
        assertEquals("https://tube.example/playlist.jpg", playlist.getThumbnails().get(0).getUrl());
        assertEquals(0, collector.getErrors().size());

        final MultiInfoItemsCollector sepia = new MultiInfoItemsCollector(3);
        PeertubeParsingHelper.collectItemsFrom(sepia, JsonParser.object().from("""
                {"data":[{"uuid":"remote-1","name":"Remote",
                  "embedUrl":"https://remote.example/peertube/videos/embed/remote-1",
                  "embedPath":"/videos/embed/remote-1",
                  "account":{"name":"alice","host":"remote.example",
                    "displayName":"Alice","avatar":{"path":"/avatar.jpg"}},
                  "thumbnailPath":"/remote.jpg"}]}
                """), "https://tube.example", true);
        final StreamInfoItem remoteVideo = (StreamInfoItem) sepia.getItems().get(0);
        assertEquals("https://remote.example/remote.jpg",
                remoteVideo.getThumbnails().get(0).getUrl());
        assertEquals("https://remote.example/avatar.jpg",
                remoteVideo.getUploaderAvatars().get(0).getUrl());
        assertEquals("https://remote.example/peertube/accounts/alice@remote.example",
                remoteVideo.getUploaderUrl());
        assertEquals("https://remote.example/peertube/videos/watch/remote-1", remoteVideo.getUrl());
    }

    @Test
    void requiresArrayResponseData() throws Exception {
        assertThrows(ParsingException.class, () -> PeertubeParsingHelper.collectItemsFrom(
                new MultiInfoItemsCollector(3), JsonParser.object().from("{\"data\":{}}"),
                "https://tube.example"));
    }

    @Test
    void sepiaItemsRejectMalformedEmbedUrls() throws Exception {
        final var item = JsonParser.object().from("""
                {"uuid":"broken","name":"Broken","embedUrl":"https://[bad"}
                """);
        final var extractor = new PeertubeSepiaStreamInfoItemExtractor(item, "https://tube.example");

        final ParsingException error = assertThrows(ParsingException.class, extractor::getUrl);
        assertEquals("Invalid Sepia video URL", error.getMessage());
    }
}
