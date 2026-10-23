// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.InfoItem;
import com.vyonex.internal.fallback.InfoItem.InfoType;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabExtractor;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCService;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCConferenceLinkHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCSearchQueryHandlerFactory;
import com.vyonex.internal.fallback.services.media_ccc.linkHandler.MediaCCCStreamLinkHandlerFactory;
import com.vyonex.internal.fallback.search.SearchExtractor;
import com.vyonex.internal.fallback.stream.DeliveryMethod;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.AudioStream;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.stream.VideoStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaCCCExtractorTest {
    static final String LIVE_STREAMS_BODY = """
            [{"conference":"Chaos","slug":"chaos-2026","description":"Conference details",
              "isCurrentlyStreaming":true,"groups":[{"group":"Hall A","rooms":[
                {"slug":"a","display":"Room A","link":"https://streaming.media.ccc.de/chaos-2026/a",
                 "talks":{"current":{"title":"Live video"}},
                 "thumb":"https://cdn.example.test/thumb.jpg","poster":"https://cdn.example.test/poster.jpg",
                 "streams":[
                   {"type":"video","videoSize":[1920,1080],"urls":{
                     "dash":{"url":"https://cdn.example.test/room.mpd","tech":"dash"},
                     "hls":{"url":"https://cdn.example.test/room.m3u8","tech":"hls"},
                     "mp4":{"url":"https://cdn.example.test/room.mp4","tech":"h264"}}},
                   {"type":"audio","urls":{"mp3":{"url":"https://cdn.example.test/room.mp3","tech":"mp3"}}}]},
                {"slug":"b","display":"Room B","link":"https://streaming.media.ccc.de/chaos-2026/b",
                 "talks":{"current":{"title":"Live audio"}},
                 "streams":[{"type":"audio"}]}]}]},
             {"conference":"Past","slug":"past","isCurrentlyStreaming":false,"groups":[]}]
            """;

    @Test
    void serviceRegistersRecentLiveAndConferenceKiosks() throws Exception {
        final MediaCCCService service = new MediaCCCService(8);

        final var kiosks = service.getKioskList();

        assertEquals(Set.of("conferences", "recent", "live"), kiosks.getAvailableKiosks());
        assertEquals("recent", kiosks.getDefaultKioskId());
    }

    @Test
    void conferenceProvidesMetadataAndAReadyVideoTab() throws Exception {
        final String conferenceBody = """
                {"title":"FOSDEM","logo_url":"https://cdn.example.test/logo.jpg",
                 "events":[{"title":"Talk","guid":"talk-1","length":900,
                   "view_count":10,"conference_title":"FOSDEM",
                   "conference_url":"https://api.media.ccc.de/public/conferences/fosdem"}]}
                """;
        final String conferenceUrl = "https://api.media.ccc.de/public/conferences/fosdem";
        final StubDownloader downloader = new StubDownloader(Map.of(conferenceUrl, conferenceBody));
        VyonEx.init(downloader);
        final MediaCCCService service = new MediaCCCService(8);
        final ListLinkHandler link = MediaCCCConferenceLinkHandlerFactory.getInstance()
                .fromUrl("https://media.ccc.de/c/fosdem");
        final ChannelExtractor conference = service.getChannelExtractor(link);

        conference.onFetchPage(downloader);

        assertEquals("FOSDEM", conference.getName());
        assertEquals("https://cdn.example.test/logo.jpg", conference.getAvatars().get(0).getUrl());
        assertEquals(1, conference.getTabs().size());
        final ChannelTabExtractor videos = service.getChannelTabExtractor(conference.getTabs().get(0));
        final List<InfoItem> items = videos.getInitialPage().getItems();
        assertEquals(1, items.size());
        assertEquals("Talk", items.get(0).getName());
        assertEquals("https://media.ccc.de/public/events/talk-1", items.get(0).getUrl());
        assertEquals(List.of(conferenceUrl), downloader.requestedUrls);
    }

    @Test
    void conferenceVideoTabMapsEventsWhenFetchedWithoutChannelContext() throws Exception {
        final String conferenceUrl = "https://api.media.ccc.de/public/conferences/fosdem";
        final String conferenceBody = """
                {"title":"FOSDEM","logo_url":"https://cdn.example.test/logo.jpg",
                 "events":[{"title":"Closing session","guid":"closing-1","length":300,
                   "view_count":4,"conference_title":"FOSDEM",
                   "conference_url":"https://media.ccc.de/c/fosdem"}]}
                """;
        final StubDownloader downloader = new StubDownloader(Map.of(conferenceUrl, conferenceBody));
        VyonEx.init(downloader);
        final MediaCCCService service = new MediaCCCService(8);
        final ListLinkHandler link = MediaCCCConferenceLinkHandlerFactory.getInstance()
                .fromUrl("https://media.ccc.de/c/fosdem");
        final ChannelTabExtractor tab = new MediaCCCChannelTabExtractor(service, link, null);

        tab.onFetchPage(downloader);

        final List<InfoItem> items = tab.getInitialPage().getItems();
        assertEquals(1, items.size());
        assertEquals("Closing session", items.get(0).getName());
        assertEquals("https://media.ccc.de/public/events/closing-1", items.get(0).getUrl());
        assertEquals(List.of(conferenceUrl), downloader.requestedUrls);
    }

    @Test
    void recordingMapsMetadataAndAudioVideoFiles() throws Exception {
        final String eventUrl = "https://api.media.ccc.de/public/events/talk-1";
        final String conferenceUrl = "https://api.media.ccc.de/public/conferences/fosdem";
        final String eventBody = """
                {"title":"Talk","guid":"talk-1","frontend_link":"https://media.ccc.de/v/talk-1",
                 "release_date":"2026-02-01T12:00:00Z","description":"A systems talk",
                 "length":1200,"view_count":34,"conference_title":"FOSDEM",
                 "conference_url":"https://api.media.ccc.de/public/conferences/fosdem",
                 "original_language":"deu","tags":["linux","systems"],"recordings":[
                   {"filename":"audio-de","recording_url":"https://cdn.example.test/audio.mp3",
                    "mime_type":"audio/mpeg","language":"de"},
                   {"filename":"video-720","recording_url":"https://cdn.example.test/video.mp4",
                    "mime_type":"video/mp4","height":720}]}
                """;
        final String conferenceBody = """
                {"logo_url":"https://cdn.example.test/logo.jpg"}
                """;
        final StubDownloader downloader = new StubDownloader(Map.of(
                eventUrl, eventBody, conferenceUrl, conferenceBody));
        VyonEx.init(downloader);
        final MediaCCCService service = new MediaCCCService(8);
        final StreamExtractor recording = service.getStreamExtractor(
                MediaCCCStreamLinkHandlerFactory.getInstance()
                        .fromUrl("https://media.ccc.de/v/talk-1"));

        recording.onFetchPage(downloader);

        assertEquals("Talk", recording.getName());
        assertEquals("https://media.ccc.de/v/talk-1", recording.getOriginalUrl());
        assertEquals("fosdem", recording.getUploaderName());
        assertEquals("https://media.ccc.de/c/fosdem", recording.getUploaderUrl());
        assertEquals(1200, recording.getLength());
        assertEquals(34, recording.getViewCount());
        assertEquals(Instant.parse("2026-02-01T12:00:00Z"),
                recording.getUploadDate().getInstant());
        assertEquals(Locale.GERMAN, recording.getLanguageInfo());
        assertEquals(List.of("linux", "systems"), recording.getTags());
        assertEquals("https://cdn.example.test/logo.jpg", recording.getUploaderAvatars()
                .get(0).getUrl());

        final List<AudioStream> audio = recording.getAudioStreams();
        assertEquals(1, audio.size());
        assertEquals("audio-de", audio.get(0).getId());
        assertEquals("https://cdn.example.test/audio.mp3", audio.get(0).getContent());
        assertEquals(Locale.GERMAN, audio.get(0).getAudioLocale());

        final List<VideoStream> video = recording.getVideoStreams();
        assertEquals(1, video.size());
        assertEquals("video-720", video.get(0).getId());
        assertEquals("720p", video.get(0).getResolution());
        assertEquals("https://cdn.example.test/video.mp4", video.get(0).getContent());
        assertEquals(List.of(eventUrl, conferenceUrl), downloader.requestedUrls);
    }

    @Test
    void recordingNormalizesConferenceWebLinkToCanonicalApiMetadata() throws Exception {
        final String eventUrl = "https://api.media.ccc.de/public/events/talk-1";
        final String conferenceApiUrl = "https://api.media.ccc.de/public/conferences/fosdem";
        final String eventBody = """
                {"title":"Talk","frontend_link":"https://media.ccc.de/v/talk-1",
                 "length":1200,"view_count":34,"conference_url":"https://media.ccc.de/c/fosdem",
                 "tags":[],"recordings":[]}
                """;
        final String conferenceBody = """
                {"logo_url":"https://cdn.example.test/logo.jpg"}
                """;
        final StubDownloader downloader = new StubDownloader(Map.of(
                eventUrl, eventBody, conferenceApiUrl, conferenceBody));
        VyonEx.init(downloader);
        final MediaCCCService service = new MediaCCCService(8);
        final StreamExtractor recording = service.getStreamExtractor(
                MediaCCCStreamLinkHandlerFactory.getInstance()
                        .fromUrl("https://media.ccc.de/v/talk-1"));

        recording.onFetchPage(downloader);

        assertEquals("fosdem", recording.getUploaderName());
        assertEquals("https://media.ccc.de/c/fosdem", recording.getUploaderUrl());
        assertEquals("https://cdn.example.test/logo.jpg", recording.getUploaderAvatars()
                .get(0).getUrl());
        assertEquals(List.of(eventUrl, conferenceApiUrl), downloader.requestedUrls);
    }

    @Test
    void recordingRejectsFailedEventResponses() throws Exception {
        final String eventUrl = "https://api.media.ccc.de/public/events/talk-1";
        final StubDownloader downloader = new StubDownloader(Map.of(eventUrl, "{}"), 503);
        VyonEx.init(downloader);
        final MediaCCCService service = new MediaCCCService(8);
        final StreamExtractor recording = service.getStreamExtractor(
                MediaCCCStreamLinkHandlerFactory.getInstance()
                        .fromUrl("https://media.ccc.de/v/talk-1"));

        final ExtractionException error = assertThrows(ExtractionException.class,
                () -> recording.onFetchPage(downloader));

        assertEquals("API request failed with HTTP 503 for " + eventUrl, error.getMessage());
    }

    @Test
    void liveRoomMapsManifestsAndDirectStreams() throws Exception {
        final String liveUrl = "https://streaming.media.ccc.de/streams/v2.json";
        final StubDownloader downloader = new StubDownloader(Map.of(liveUrl, LIVE_STREAMS_BODY));
        VyonEx.init(downloader);
        final MediaCCCService service = new MediaCCCService(8);
        final StreamExtractor live = service.getStreamExtractor(
                MediaCCCStreamLinkHandlerFactory.getInstance()
                        .fromUrl("https://streaming.media.ccc.de/chaos-2026/a"));

        live.onFetchPage(downloader);

        assertEquals("Room A", live.getName());
        assertEquals("https://streaming.media.ccc.de/chaos-2026", live.getUploaderUrl());
        assertEquals("Chaos", live.getUploaderName());
        assertEquals("Hall A", live.getCategory());
        assertEquals("Conference details - Hall A", live.getDescription().getContent());
        assertEquals("https://cdn.example.test/room.mpd", live.getDashMpdUrl());
        assertEquals("https://cdn.example.test/room.m3u8", live.getHlsUrl());
        assertEquals(1, live.getAudioStreams().size());
        assertEquals("https://cdn.example.test/room.mp3", live.getAudioStreams().get(0).getContent());
        assertEquals(2, live.getVideoStreams().size());
        assertEquals(DeliveryMethod.HLS, live.getVideoStreams().get(0).getDeliveryMethod());
        assertEquals("1920x1080", live.getVideoStreams().get(0).getResolution());
        assertEquals("https://cdn.example.test/room.mp4", live.getVideoStreams().get(1).getContent());
        assertEquals(Description.PLAIN_TEXT, live.getDescription().getType());
    }

    @Test
    void liveRoomRejectsUnknownConferenceAndRoomIds() throws Exception {
        final String liveUrl = "https://streaming.media.ccc.de/streams/v2.json";
        final StubDownloader downloader = new StubDownloader(Map.of(liveUrl, LIVE_STREAMS_BODY));
        VyonEx.init(downloader);
        final MediaCCCService service = new MediaCCCService(8);
        final StreamExtractor live = service.getStreamExtractor(
                MediaCCCStreamLinkHandlerFactory.getInstance()
                        .fromUrl("https://streaming.media.ccc.de/chaos-2026/missing"));

        final ExtractionException error = assertThrows(ExtractionException.class,
                () -> live.onFetchPage(downloader));

        assertEquals("No live room matches chaos-2026/missing", error.getMessage());
        assertEquals(List.of(liveUrl), downloader.requestedUrls);
    }

    @Test
    void searchCombinesMatchingConferencesAndReleasedEvents() throws Exception {
        final String eventsUrl = "https://media.ccc.de/public/events/search?q=fos";
        final String conferencesUrl = "https://media.ccc.de/public/conferences";
        final String eventsBody = """
                {"events":[
                  {"title":"FOSDEM opening","guid":"talk-search","length":900,
                   "conference_title":"FOSDEM","conference_url":"https://api.media.ccc.de/public/conferences/fosdem",
                   "release_date":"2026-02-01T12:00:00Z"},
                  {"title":"Future session","guid":"future","release_date":null}]}
                """;
        final String conferencesBody = """
                {"Conferences":"Conferences","conferences":[
                  {"title":"FOSDEM","url":"https://media.ccc.de/c/fosdem"},
                  {"title":"Another event","url":"https://media.ccc.de/c/another"}]}
                """;
        final StubDownloader downloader = new StubDownloader(Map.of(
                eventsUrl, eventsBody, conferencesUrl, conferencesBody));
        VyonEx.init(downloader);
        final MediaCCCService service = new MediaCCCService(8);
        final SearchExtractor search = service.getSearchExtractor(
                MediaCCCSearchQueryHandlerFactory.getInstance()
                        .fromQuery("fos", List.of(MediaCCCSearchQueryHandlerFactory.ALL), ""));

        search.onFetchPage(downloader);

        final List<InfoItem> items = search.getInitialPage().getItems();
        assertEquals(2, items.size());
        assertEquals(InfoType.CHANNEL, items.get(0).getInfoType());
        assertEquals("FOSDEM", items.get(0).getName());
        assertEquals(InfoType.STREAM, items.get(1).getInfoType());
        assertEquals("FOSDEM opening", items.get(1).getName());
        assertEquals(List.of(eventsUrl, conferencesUrl), downloader.requestedUrls);
    }

    private static final class StubDownloader extends Downloader {
        private final Map<String, String> bodies;
        private final int responseCode;
        private final List<String> requestedUrls = new ArrayList<>();

        private StubDownloader(final Map<String, String> bodies) { this(bodies, 200); }

        private StubDownloader(final Map<String, String> bodies, final int responseCode) {
            this.bodies = bodies;
            this.responseCode = responseCode;
        }

        @Override
        public Response execute(final Request request) throws IOException, ReCaptchaException {
            requestedUrls.add(request.url());
            final String body = bodies.get(request.url());
            if (body == null) throw new IOException("No offline response for " + request.url());
            return new Response(responseCode, "OK", Map.of(), body, request.url());
        }
    }
}
