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
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

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
    void reportsUnavailableResponseErrors() throws Exception {
        assertThrows(ContentNotAvailableException.class, () -> PeertubeParsingHelper.validate(
                JsonParser.object().from("{\"error\":\"Video unavailable\"}")));
        PeertubeParsingHelper.validate(JsonParser.object().from("{\"data\":[]}"));
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
                   "account":{"name":"alice","host":"tube.example","displayName":"Alice"},
                   "publishedAt":"2026-01-02T03:04:05Z","thumbnailPath":"/video.jpg"},
                  {"displayName":"Channel","url":"https://tube.example/video-channels/c1",
                   "followersCount":7},
                  {"displayName":"Playlist","url":"https://tube.example/videos/watch/playlist",
                   "videosLength":2,"uploader":{"displayName":"Alice","url":"https://tube.example/a/alice"}}
                ]}
                """;
        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(3);

        PeertubeParsingHelper.collectItemsFrom(collector, JsonParser.object().from(response),
                "https://tube.example", false);

        assertEquals(3, collector.getItems().size());
        assertEquals(InfoItem.InfoType.STREAM, collector.getItems().get(0).getInfoType());
        assertEquals("https://tube.example/videos/watch/video-1", collector.getItems().get(0).getUrl());
        assertEquals(InfoItem.InfoType.CHANNEL, collector.getItems().get(1).getInfoType());
        assertEquals(InfoItem.InfoType.PLAYLIST, collector.getItems().get(2).getInfoType());
        assertEquals(0, collector.getErrors().size());

        final MultiInfoItemsCollector sepia = new MultiInfoItemsCollector(3);
        PeertubeParsingHelper.collectItemsFrom(sepia, JsonParser.object().from("""
                {"data":[{"uuid":"remote-1","name":"Remote",
                  "embedUrl":"https://remote.example/videos/embed/remote-1"}]}
                """), "https://tube.example", true);
        assertEquals("https://remote.example/videos/watch/remote-1", sepia.getItems().get(0).getUrl());
    }

    @Test
    void requiresArrayResponseData() throws Exception {
        assertThrows(ParsingException.class, () -> PeertubeParsingHelper.collectItemsFrom(
                new MultiInfoItemsCollector(3), JsonParser.object().from("{\"data\":{}}"),
                "https://tube.example"));
    }
}
