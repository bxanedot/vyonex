// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud;

import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudChannelInfoItemExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudCommentsInfoItemExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudPlaylistInfoItemExtractor;
import com.vyonex.internal.fallback.services.soundcloud.extractors.SoundcloudStreamInfoItemExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudChannelTabLinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudChartsLinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudPlaylistLinkHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudSearchQueryHandlerFactory;
import com.vyonex.internal.fallback.services.soundcloud.linkHandler.SoundcloudStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundcloudMappingTest {
    @Test
    void mapsTrackChannelPlaylistAndCommentFields() throws Exception {
        final var track = JsonParser.object().from("""
                {"id":42,"title":"Song","permalink_url":"http://soundcloud.com/a/song",
                 "duration":125000,"created_at":"2026-10-22T12:30:00Z","playback_count":7,
                 "artwork_url":"https://images.example/song.jpg",
                 "user":{"username":"Alice","permalink_url":"http://soundcloud.com/alice",
                   "avatar_url":"https://images.example/alice.jpg","verified":true}}
                """);
        final SoundcloudStreamInfoItemExtractor stream = new SoundcloudStreamInfoItemExtractor(track);
        assertEquals("Song", stream.getName());
        assertEquals("https://soundcloud.com/a/song", stream.getUrl());
        assertEquals(125, stream.getDuration());
        assertEquals("Alice", stream.getUploaderName());
        assertTrue(stream.isUploaderVerified());
        assertEquals(7, stream.getViewCount());
        assertEquals(StreamType.AUDIO_STREAM, stream.getStreamType());

        final var user = JsonParser.object().from("""
                {"username":"Alice","permalink_url":"http://soundcloud.com/alice",
                 "avatar_url":"https://images.example/alice.jpg","followers_count":10,
                 "track_count":4,"verified":true,"description":"Producer"}
                """);
        final SoundcloudChannelInfoItemExtractor channel = new SoundcloudChannelInfoItemExtractor(user);
        assertEquals("Alice", channel.getName());
        assertEquals("https://soundcloud.com/alice", channel.getUrl());
        assertEquals(10, channel.getSubscriberCount());
        assertEquals(4, channel.getStreamCount());
        assertEquals("Producer", channel.getDescription());

        final var playlistJson = JsonParser.object().from("""
                {"title":"Mix","permalink_url":"http://soundcloud.com/alice/sets/mix",
                 "track_count":1,"tracks":[{"artwork_url":"https://images.example/song.jpg"}],
                 "user":{"username":"Alice","permalink_url":"http://soundcloud.com/alice"}}
                """);
        final SoundcloudPlaylistInfoItemExtractor playlist =
                new SoundcloudPlaylistInfoItemExtractor(playlistJson);
        assertEquals("Mix", playlist.getName());
        assertEquals("https://soundcloud.com/alice/sets/mix", playlist.getUrl());
        assertEquals("Alice", playlist.getUploaderName());
        assertEquals("https://soundcloud.com/alice", playlist.getUploaderUrl());
        assertEquals(1, playlist.getStreamCount());
        assertEquals("https://images.example/song.jpg", playlist.getThumbnails().get(0).getUrl());

        final var commentJson = JsonParser.object().from("""
                {"id":9,"body":"Nice","timestamp":4200,"created_at":"2026-10-22T12:30:00Z",
                 "user":{"username":"Alice","permalink":"alice",
                   "permalink_url":"http://soundcloud.com/alice",
                   "avatar_url":"https://images.example/alice.jpg","verified":true}}
                """);
        final SoundcloudCommentsInfoItemExtractor comment =
                new SoundcloudCommentsInfoItemExtractor(commentJson, "https://soundcloud.com/comment/9");
        assertEquals("9", comment.getCommentId());
        assertEquals("Nice", comment.getCommentText().getContent());
        assertEquals("Alice", comment.getUploaderName());
        assertEquals("https://soundcloud.com/alice", comment.getUploaderUrl());
        assertTrue(comment.isUploaderVerified());
        assertEquals(4, comment.getStreamPosition());
        assertEquals("alice", comment.getName());
        assertEquals("2026-10-22T12:30:00Z", comment.getTextualUploadDate());
        assertEquals("https://images.example/alice.jpg", comment.getUploaderAvatars().get(0).getUrl());
    }

    @Test
    void acceptsServiceRoutesAndExposesStableTabAndSearchFilters() {
        assertTrue(SoundcloudChannelLinkHandlerFactory.getInstance()
                .onAcceptUrl("https://soundcloud.com/alice/sets"));
        assertTrue(SoundcloudStreamLinkHandlerFactory.getInstance()
                .onAcceptUrl("https://api-v2.soundcloud.com/tracks/42"));
        assertTrue(SoundcloudPlaylistLinkHandlerFactory.getInstance()
                .onAcceptUrl("https://soundcloud.com/alice/sets/mix"));
        assertTrue(SoundcloudChartsLinkHandlerFactory.getInstance()
                .onAcceptUrl("https://soundcloud.com/charts/new"));
        assertFalse(SoundcloudStreamLinkHandlerFactory.getInstance()
                .onAcceptUrl("https://example.com/alice/song"));
        assertEquals("/tracks", SoundcloudChannelTabLinkHandlerFactory
                .getUrlSuffix(ChannelTabs.TRACKS));
        assertEquals(List.of("all", "tracks", "users", "playlists"),
                List.of(SoundcloudSearchQueryHandlerFactory.getInstance().getAvailableContentFilter()));
    }
}
