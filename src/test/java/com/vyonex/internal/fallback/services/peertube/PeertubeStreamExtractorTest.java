// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.peertube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.stream.DeliveryMethod;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeStreamLinkHandlerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PeertubeStreamExtractorTest {
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
    void treatsMissingOptionalSubresourcesDifferentlyFromServerFailures() throws Exception {
        final String base = "https://tube.example";
        final String videoUrl = base + "/api/v1/videos/video-1";
        final String chaptersUrl = videoUrl + "/chapters";
        final String storyboardsUrl = videoUrl + "/storyboards";
        final Map<String, Response> responses = Map.of(
                videoUrl, new Response(200, "OK", Map.of(),
                        "{\"name\":\"Video\",\"isLive\":false}", videoUrl),
                videoUrl + "/captions", new Response(200, "OK", Map.of(), "{\"data\":[]}",
                        videoUrl + "/captions"),
                chaptersUrl, new Response(400, "Bad Request", Map.of(), "", chaptersUrl),
                storyboardsUrl, new Response(503, "Unavailable", Map.of(), "", storyboardsUrl));
        VyonEx.init(new Downloader() {
            @Override public Response execute(final Request request)
                    throws IOException, ReCaptchaException {
                final Response response = responses.get(request.url());
                if (response == null) throw new IOException("No offline response for " + request.url());
                return response;
            }
        });
        final PeertubeService service = new PeertubeService(3,
                new PeertubeInstance(base, "Offline"));
        final var handler = PeertubeStreamLinkHandlerFactory.getInstance()
                .fromUrl(base + "/videos/watch/video-1");
        final var stream = service.getStreamExtractor(handler);
        stream.fetchPage();

        assertTrue(stream.getStreamSegments().isEmpty());
        assertThrows(ExtractionException.class, stream::getFrames);
    }

    @Test
    void liveStreamsExposeTheirHlsPlaylistWithoutSeparateAudio() throws Exception {
        final String base = "https://tube.example";
        final String videoUrl = base + "/api/v1/videos/live-1";
        final StubDownloader downloader = new StubDownloader(Map.of(
                videoUrl, """
                        {"name":"Live","isLive":true,
                         "streamingPlaylists":[{"id":4,
                           "playlistUrl":"https://cdn.example/live.m3u8"}]}
                        """,
                videoUrl + "/captions", "{\"data\":[]}"));
        VyonEx.init(downloader);
        final PeertubeService service = new PeertubeService(3,
                new PeertubeInstance(base, "Offline"));
        final var handler = PeertubeStreamLinkHandlerFactory.getInstance()
                .fromUrl(base + "/videos/watch/live-1");
        final var stream = service.getStreamExtractor(handler);
        stream.fetchPage();

        assertEquals(StreamType.LIVE_STREAM, stream.getStreamType());
        assertEquals(1, stream.getVideoStreams().size());
        assertEquals("https://cdn.example/live.m3u8", stream.getVideoStreams().get(0).getContent());
        assertEquals(DeliveryMethod.HLS, stream.getVideoStreams().get(0).getDeliveryMethod());
        assertTrue(stream.getAudioStreams().isEmpty());
    }

    @Test
    void sameQualityFileVariantsKeepTheirSourceInTheStreamId() throws Exception {
        final String base = "https://tube.example";
        final String videoUrl = base + "/api/v1/videos/video-1";
        final StubDownloader downloader = new StubDownloader(Map.of(
                videoUrl, """
                        {"name":"Variants","isLive":false,"files":[
                          {"fileUrl":"https://cdn.example/720.mp4",
                           "resolution":{"id":720,"label":"720p"}},
                          {"fileDownloadUrl":"https://cdn.example/download/720.mp4",
                           "resolution":{"id":720,"label":"720p"}}]}
                        """,
                videoUrl + "/captions", "{\"data\":[]}"));
        VyonEx.init(downloader);
        final PeertubeService service = new PeertubeService(3,
                new PeertubeInstance(base, "Offline"));
        final var handler = PeertubeStreamLinkHandlerFactory.getInstance()
                .fromUrl(base + "/videos/watch/video-1");
        final var stream = service.getStreamExtractor(handler);
        stream.fetchPage();

        final var variants = stream.getVideoStreams();
        assertEquals(2, variants.size());
        assertEquals("720p-mp4", variants.get(0).getId());
        assertEquals("https://cdn.example/720.mp4", variants.get(0).getContent());
        assertEquals("720p-mp4-fileDownloadUrl", variants.get(1).getId());
        assertEquals("https://cdn.example/download/720.mp4", variants.get(1).getContent());
    }

    @Test
    void mapsMetadataMediaCaptionsChaptersStoryboardsAndRelatedVideosOffline() throws Exception {
        final String base = "https://tube.example";
        final String videoUrl = base + "/api/v1/videos/video-1";
        final String captionsUrl = videoUrl + "/captions";
        final String chaptersUrl = videoUrl + "/chapters";
        final String storyboardsUrl = videoUrl + "/storyboards";
        final String relatedUrl = base
                + "/api/v1/search/videos?start=0&count=8&sort=-createdAt&tagsOneOf=music";
        final String video = """
                {"name":"A PeerTube video","publishedAt":"2026-01-02T03:04:05Z",
                 "description":"A clean description","duration":123,"views":99,"likes":8,
                 "dislikes":1,"nsfw":true,"isLive":false,"support":"Support link",
                 "tags":["music"],"account":{"name":"alice","host":"tube.example",
                   "displayName":"Alice","avatar":{"path":"/alice.png"}},
                 "channel":{"displayName":"Music","url":"https://tube.example/c/music"},
                 "privacy":{"id":1},"category":{"label":"Music"},
                 "licence":{"label":"CC BY"},"language":{"id":"en"},
                 "thumbnailPath":"/video.jpg",
                 "files":[
                   {"fileUrl":"https://cdn.example/video-720.mp4",
                    "resolution":{"id":720,"label":"720p"},"hasAudio":true,
                    "torrentUrl":"https://cdn.example/video-720.torrent"},
                   {"fileUrl":"https://cdn.example/audio.webm",
                    "resolution":{"id":0,"label":"audio"}}],
                 "streamingPlaylists":[{"playlistUrl":"https://cdn.example/master.m3u8",
                   "files":[
                     {"fileUrl":"https://cdn.example/video-720-fragmented.mp4",
                      "resolution":{"id":720,"label":"720p"},"hasAudio":true},
                     {"fileUrl":"https://cdn.example/audio-fragmented.webm",
                      "resolution":{"id":0,"label":"audio"}}]}]}
                """;
        final StubDownloader downloader = new StubDownloader(Map.of(
                videoUrl, video,
                captionsUrl, """
                        {"data":[{"captionPath":"/captions/en.vtt","language":{"id":"en"}}]}
                        """,
                chaptersUrl, """
                        {"chapters":[{"title":"Introduction","timecode":30}]}
                        """,
                storyboardsUrl, """
                        {"storyboards":[{"storyboardPath":"/sprites/video.jpg",
                          "spriteWidth":160,"spriteHeight":90,"totalWidth":320,
                          "totalHeight":180,"spriteDuration":10}]}
                        """,
                relatedUrl, """
                        {"data":[
                          {"uuid":"video-1","name":"A PeerTube video"},
                          {"uuid":"video-2","name":"Related video","views":3,
                           "account":{"name":"alice","host":"tube.example",
                             "displayName":"Alice"}}]}
                        """));
        VyonEx.init(downloader);
        final PeertubeService service = new PeertubeService(3,
                new PeertubeInstance(base, "Offline"));
        final var handler = PeertubeStreamLinkHandlerFactory.getInstance()
                .fromUrl(base + "/videos/watch/video-1?start=1h2m3s");
        final var stream = service.getStreamExtractor(handler);
        stream.fetchPage();

        assertEquals("A PeerTube video", stream.getName());
        assertEquals(123, stream.getLength());
        assertEquals(99, stream.getViewCount());
        assertEquals(8, stream.getLikeCount());
        assertEquals(1, stream.getDislikeCount());
        assertEquals(18, stream.getAgeLimit());
        assertEquals(3723, stream.getTimeStamp());
        assertEquals(StreamType.VIDEO_STREAM, stream.getStreamType());
        assertEquals("Alice", stream.getUploaderName());
        assertEquals("Music", stream.getSubChannelName());
        assertEquals("Support link", stream.getSupportInfo());
        assertEquals("Music", stream.getCategory());
        assertEquals("CC BY", stream.getLicence());
        assertEquals("https://cdn.example/master.m3u8", stream.getHlsUrl());
        assertEquals("https://tube.example/video.jpg", stream.getThumbnails().get(0).getUrl());
        assertTrue(stream.getAudioStreams().stream()
                .anyMatch(item -> item.getDeliveryMethod() == DeliveryMethod.HLS));
        assertTrue(stream.getVideoStreams().stream()
                .anyMatch(item -> item.getDeliveryMethod() == DeliveryMethod.HLS));
        assertTrue(stream.getAudioStreams().stream()
                .anyMatch(item -> item.getId().equals("audio-hls")));
        assertTrue(stream.getVideoStreams().stream()
                .anyMatch(item -> item.getId().equals("720p-hls")));
        assertTrue(stream.getVideoStreams().stream()
                .anyMatch(item -> item.getId().equals("720p-torrent")));
        assertEquals("https://tube.example/captions/en.vtt",
                stream.getSubtitlesDefault().get(0).getContent());
        assertFalse(stream.getSubtitlesDefault().get(0).isAutoGenerated());
        assertEquals("Introduction", stream.getStreamSegments().get(0).getTitle());
        assertEquals(4, stream.getFrames().get(0).getTotalCount());
        final var related = stream.getRelatedItems();
        assertEquals(1, related.getItems().size());
        assertEquals("Related video", related.getItems().get(0).getName());
        assertNotNull(stream.getUploadDate());
        assertEquals(List.of(videoUrl, captionsUrl, chaptersUrl, storyboardsUrl, relatedUrl),
                downloader.requestedUrls);
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
