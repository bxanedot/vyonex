// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.ibm.icu.util.ULocale;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.fallback.stream.ContentAvailability;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeStreamInfoItemLockupExtractorContractTest {
    @Test
    void mapsVideoDetailsCreatorImagesAndDuration() throws Exception {
        final var extractor = new YoutubeStreamInfoItemLockupExtractor(JsonParser.object().from("""
                {
                  "contentId":"abcdefghijk",
                  "metadata":{"lockupMetadataViewModel":{
                    "title":{"content":"Video title"},
                    "metadata":{"contentMetadataViewModel":{"metadataRows":[
                      {"metadataParts":[{"text":{"content":"Creator","attachmentRuns":[]}}]},
                      {"metadataParts":[{"text":{"content":"1.2K views"}},{"text":{"content":"2 days ago"}}]}
                    ]}},
                    "image":{"decoratedAvatarViewModel":{
                      "avatar":{"avatarViewModel":{"image":{"sources":[{"url":"//img.example/avatar.jpg","width":48,"height":48}]}}},
                      "rendererContext":{"commandContext":{"onTap":{"innertubeCommand":{"browseEndpoint":{"browseId":"UC123"}}}}}
                    }}
                  }},
                  "contentImage":{"thumbnailViewModel":{
                    "image":{"sources":[{"url":"//img.example/video.jpg","width":480,"height":270}]},
                    "overlays":[{"thumbnailBottomOverlayViewModel":{"badges":[
                      {"thumbnailBadgeViewModel":{"text":"4:20"}}
                    ]}}]
                  }}
                }
                """), null);

        assertEquals("Video title", extractor.getName());
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", extractor.getUrl());
        assertEquals(StreamType.VIDEO_STREAM, extractor.getStreamType());
        assertEquals(260, extractor.getDuration());
        assertEquals("Creator", extractor.getUploaderName());
        assertEquals("https://www.youtube.com/channel/UC123", extractor.getUploaderUrl());
        assertEquals("https://img.example/avatar.jpg", extractor.getUploaderAvatars().get(0).getUrl());
        assertEquals("https://img.example/video.jpg", extractor.getThumbnails().get(0).getUrl());
        assertEquals(1200, extractor.getViewCount());
        assertEquals("2 days ago", extractor.getTextualUploadDate());
        assertNull(extractor.getUploadDate());
        assertEquals(ContentAvailability.AVAILABLE, extractor.getContentAvailability());
        assertFalse(extractor.isAd());
        assertFalse(extractor.isUploaderVerified());
    }

    @Test
    void recognizesPremiereDatesAndWaitingCounts() throws Exception {
        final var extractor = new YoutubeStreamInfoItemLockupExtractor(JsonParser.object().from("""
                {
                  "metadata":{"lockupMetadataViewModel":{
                    "title":{"content":"Upcoming"},
                    "metadata":{"contentMetadataViewModel":{"metadataRows":[
                      {"metadataParts":[{"text":{"content":"Creator"}}]},
                      {"metadataParts":[{"text":{"content":"5K waiting"}},{"text":{"content":"Premieres 23/10/2026, 17:45"}}]}
                    ]}}
                  }},
                  "contentImage":{"thumbnailViewModel":{"overlays":[{"thumbnailBottomOverlayViewModel":{"badges":[
                    {"thumbnailBadgeViewModel":{"text":"12:34"}}
                  ]}}]}}
                }
                """), new TimeAgoParser(ULocale.ENGLISH, LocalDateTime.of(2026, 10, 23, 12, 0)));

        assertEquals(-1, extractor.getDuration());
        assertEquals(-1, extractor.getViewCount());
        assertEquals("23/10/2026, 17:45", extractor.getTextualUploadDate());
        assertEquals(Instant.parse("2026-10-23T17:45:00Z"), extractor.getUploadDate().getInstant());
        assertEquals(ContentAvailability.UPCOMING, extractor.getContentAvailability());
    }

    @Test
    void handlesLiveAndMembersOnlyCardsAndRejectsMissingRequiredData() throws Exception {
        final var live = new YoutubeStreamInfoItemLockupExtractor(JsonParser.object().from("""
                {
                  "contentId":"abcdefghijk",
                  "metadata":{"lockupMetadataViewModel":{"title":{"content":"Live"}}},
                  "contentImage":{"thumbnailViewModel":{"overlays":[{"thumbnailOverlayBadgeViewModel":{"thumbnailBadges":[
                    {"thumbnailBadgeViewModel":{"badgeStyle":"THUMBNAIL_OVERLAY_BADGE_STYLE_LIVE"}}
                  ]}}]}}
                }
                """), null) {
            @Override protected boolean isChannelOrCoursePlaylistLockupItem() { return true; }
        };
        final var members = new YoutubeStreamInfoItemLockupExtractor(JsonParser.object().from("""
                {
                  "metadata":{"lockupMetadataViewModel":{
                    "title":{"content":"Members video"},
                    "metadata":{"contentMetadataViewModel":{"metadataRows":[
                      {"badges":[{"badgeViewModel":{"badgeStyle":"BADGE_MEMBERS_ONLY"}}],
                       "metadataParts":[{"text":{"content":"2 days ago"}}]}
                    ]}}
                  }}
                }
                """), null);
        final var missing = new YoutubeStreamInfoItemLockupExtractor(JsonParser.object().from("{}"), null);

        assertEquals(StreamType.LIVE_STREAM, live.getStreamType());
        assertEquals(-1, live.getDuration());
        assertEquals(0, live.getViewCount());
        assertNull(live.getTextualUploadDate());
        assertEquals(ContentAvailability.AVAILABLE, live.getContentAvailability());
        assertEquals(-1, members.getViewCount());
        assertEquals(ContentAvailability.MEMBERSHIP, members.getContentAvailability());
        assertThrows(ParsingException.class, missing::getName);
        assertThrows(ParsingException.class, missing::getUrl);
    }
}
