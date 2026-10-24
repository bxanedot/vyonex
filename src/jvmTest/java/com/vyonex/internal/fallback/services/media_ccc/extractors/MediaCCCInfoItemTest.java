// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors;

import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLiveConferencePayload;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLiveRoomPayload;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLiveStreamPayload;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLiveTalkPayload;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCLiveTalksPayload;
import com.vyonex.internal.fallback.services.media_ccc.extractors.infoItems.MediaCCCConferenceInfoItemExtractor;
import com.vyonex.internal.fallback.services.media_ccc.extractors.infoItems.MediaCCCStreamInfoItemExtractor;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCEventPayload;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCConferenceSummaryPayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaCCCInfoItemTest {
    @Test
    void mapsConferenceAndEventApiFields() throws Exception {
        final MediaCCCConferenceInfoItemExtractor channel =
                new MediaCCCConferenceInfoItemExtractor(new MediaCCCConferenceSummaryPayload(
                        "FOSDEM", "https://media.ccc.de/c/fosdem",
                        "https://cdn.example.test/fosdem.jpg"));

        assertEquals("FOSDEM", channel.getName());
        assertEquals("https://media.ccc.de/c/fosdem", channel.getUrl());
        assertEquals("https://cdn.example.test/fosdem.jpg", channel.getThumbnails().get(0).getUrl());

        final MediaCCCEventPayload event = MediaCCCEventPayload.Companion.decode("""
                {"length":1800,"view_count":345,"conference_title":"FOSDEM",
                 "conference_url":"https://media.ccc.de/c/fosdem",
                 "release_date":"2026-02-01T12:00:00Z","title":"A VyonEx talk",
                 "guid":"talk-1","thumb_url":"https://cdn.example.test/thumb.jpg"}
                """);
        final MediaCCCStreamInfoItemExtractor stream = new MediaCCCStreamInfoItemExtractor(event);

        assertEquals("A VyonEx talk", stream.getName());
        assertEquals("https://media.ccc.de/public/events/talk-1", stream.getUrl());
        assertEquals(1800, stream.getDuration());
        assertEquals(345, stream.getViewCount());
        assertEquals("FOSDEM", stream.getUploaderName());
        assertEquals(StreamType.VIDEO_STREAM, stream.getStreamType());
        assertFalse(stream.isAd());
        assertEquals("https://cdn.example.test/thumb.jpg", stream.getThumbnails().get(0).getUrl());
        assertEquals("2026-02-01T12:00:00Z", stream.getTextualUploadDate());
    }

    @Test
    void mapsRecentAndLiveRoomFields() throws Exception {
        final MediaCCCEventPayload recentEvent = MediaCCCEventPayload.Companion.decode("""
                {"title":"Recent talk","frontend_link":"https://media.ccc.de/v/talk-2",
                 "poster_url":"https://cdn.example.test/poster.jpg","duration":600,
                 "view_count":17,"conference_title":"Chaos","conference_url":"https://api.media.ccc.de/public/conferences/chaos",
                 "date":"2026-02-01T12:00:00.000UTC"}
                """);
        final MediaCCCRecentKioskExtractor recent = new MediaCCCRecentKioskExtractor(recentEvent);

        assertEquals("Recent talk", recent.getName());
        assertEquals("https://media.ccc.de/v/talk-2", recent.getUrl());
        assertEquals("https://media.ccc.de/c/chaos", recent.getUploaderUrl());
        assertEquals(600, recent.getDuration());
        assertEquals(17, recent.getViewCount());
        assertEquals("https://cdn.example.test/poster.jpg", recent.getThumbnails().get(0).getUrl());
        assertEquals("2026-02-01T12:00:00.000UTC", recent.getTextualUploadDate());
        assertEquals(DateWrapper.fromInstant("2026-02-01T12:00:00Z").getInstant(),
                recent.getUploadDate().getInstant());
        final MediaCCCRecentKioskExtractor offsetDate = new MediaCCCRecentKioskExtractor(
                MediaCCCEventPayload.Companion.decode("{\"date\":\"2026-02-01T12:00:00.000+02:00\"}"));
        assertEquals(DateWrapper.fromInstant("2026-02-01T10:00:00Z").getInstant(),
                offsetDate.getUploadDate().getInstant());

        final MediaCCCRecentKioskExtractor undated = new MediaCCCRecentKioskExtractor(
                MediaCCCEventPayload.Companion.decode("{}"));
        assertNull(undated.getUploadDate());
        final MediaCCCRecentKioskExtractor invalidDate = new MediaCCCRecentKioskExtractor(
                MediaCCCEventPayload.Companion.decode("{\"date\":\"not-a-date\"}"));
        assertThrows(ParsingException.class, invalidDate::getUploadDate);

        final MediaCCCLiveConferencePayload conference = new MediaCCCLiveConferencePayload(
                "Chaos", "chaos-2026", null, true, null);
        final MediaCCCLiveRoomPayload videoRoom = new MediaCCCLiveRoomPayload(
                null, "Hall 1", "https://streaming.media.ccc.de/chaos-2026/hall-1",
                "thumb.jpg", "poster.jpg", new MediaCCCLiveTalksPayload(
                        new MediaCCCLiveTalkPayload("Now live"), null),
                List.of(new MediaCCCLiveStreamPayload("audio", null, null),
                        new MediaCCCLiveStreamPayload("video", null, null)));
        final MediaCCCLiveStreamKioskExtractor live =
                new MediaCCCLiveStreamKioskExtractor(conference, "Stage", videoRoom);

        assertEquals("Now live", live.getName());
        assertEquals("https://streaming.media.ccc.de/chaos-2026/hall-1", live.getUrl());
        assertEquals("Chaos - Stage - Hall 1", live.getUploaderName());
        assertEquals("https://media.ccc.de/c/chaos-2026", live.getUploaderUrl());
        assertEquals(StreamType.LIVE_STREAM, live.getStreamType());
        assertNull(live.getUploadDate());

        final MediaCCCLiveRoomPayload audioRoom = new MediaCCCLiveRoomPayload(
                null, null, null, null, null, null,
                List.of(new MediaCCCLiveStreamPayload("audio", null, null)));
        assertEquals(StreamType.AUDIO_LIVE_STREAM,
                new MediaCCCLiveStreamKioskExtractor(conference, "Stage", audioRoom).getStreamType());
    }

    @Test
    void rejectsEventItemsWithoutTitleOrGuid() throws Exception {
        final MediaCCCEventPayload noTitle = MediaCCCEventPayload.Companion.decode("{\"guid\":\"talk-1\"}");
        final MediaCCCEventPayload noGuid = MediaCCCEventPayload.Companion.decode("{\"title\":\"Talk\"}");

        assertThrows(ParsingException.class,
                () -> new MediaCCCStreamInfoItemExtractor(noTitle).getName());
        assertThrows(ParsingException.class,
                () -> new MediaCCCStreamInfoItemExtractor(noGuid).getUrl());
    }
}
