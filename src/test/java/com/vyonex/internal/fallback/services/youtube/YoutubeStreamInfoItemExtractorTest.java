// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.services.youtube.extractors.YoutubeStreamInfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.ContentAvailability;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeStreamInfoItemExtractorTest {
    @Test
    void readsVideoMetadataAndCountsWithoutNetwork() throws Exception {
        final JsonObject item = json(
                "videoId", "12345678901",
                "title", json("simpleText", "A track"),
                "lengthText", json("simpleText", "3:14"),
                "longBylineText", byline("Creator", "UC123"),
                "ownerBadges", array(json("metadataBadgeRenderer",
                        json("style", "BADGE_STYLE_TYPE_VERIFIED"))),
                "channelThumbnailSupportedRenderers", json("channelThumbnailWithLinkRenderer",
                        json("thumbnail", json("thumbnails", array(thumbnail("creator.jpg"))))),
                "publishedTimeText", json("simpleText", "2 days ago"),
                "viewCountText", json("simpleText", "1,234 views"),
                "thumbnail", json("thumbnails", array(thumbnail("video.jpg"))),
                "descriptionSnippet", json("simpleText", "Track description"));

        final YoutubeStreamInfoItemExtractor extractor = new YoutubeStreamInfoItemExtractor(item, null);
        assertEquals(StreamType.VIDEO_STREAM, extractor.getStreamType());
        assertEquals("A track", extractor.getName());
        assertEquals("https://www.youtube.com/watch?v=12345678901", extractor.getUrl());
        assertEquals(194, extractor.getDuration());
        assertEquals("Creator", extractor.getUploaderName());
        assertEquals("https://www.youtube.com/channel/UC123", extractor.getUploaderUrl());
        assertTrue(extractor.isUploaderVerified());
        assertEquals("2 days ago", extractor.getTextualUploadDate());
        assertNull(extractor.getUploadDate());
        assertEquals(1234, extractor.getViewCount());
        assertEquals("https://img.example/creator.jpg", extractor.getUploaderAvatars().get(0).getUrl());
        assertEquals("https://img.example/video.jpg", extractor.getThumbnails().get(0).getUrl());
        assertEquals("Track description", extractor.getShortDescription());
        assertEquals(ContentAvailability.AVAILABLE, extractor.getContentAvailability());
        assertFalse(extractor.isAd());
        assertEquals(0, new YoutubeStreamInfoItemExtractor(
                json("viewCountText", json("simpleText", "No views")), null).getViewCount());
    }

    @Test
    void handlesLiveStreamsAndTheirViewerCountFallback() throws Exception {
        final JsonObject item = json(
                "title", json("simpleText", "Live"),
                "lengthText", json("simpleText", "24:00"),
                "publishedTimeText", json("simpleText", "yesterday"),
                "shortViewCountText", json("simpleText", "1.2K watching"),
                "thumbnailOverlays", array(json("thumbnailOverlayTimeStatusRenderer",
                        json("style", "LIVE"))));
        final YoutubeStreamInfoItemExtractor extractor = new YoutubeStreamInfoItemExtractor(item, null);

        assertEquals(StreamType.LIVE_STREAM, extractor.getStreamType());
        assertEquals(-1, extractor.getDuration());
        assertNull(extractor.getTextualUploadDate());
        assertNull(extractor.getUploadDate());
        assertEquals(1200, extractor.getViewCount());
    }

    @Test
    void recognizesPremieresAndMembershipAvailability() throws Exception {
        final long startTime = 1_800_000_000L;
        final JsonObject item = json(
                "title", json("simpleText", "Upcoming"),
                "upcomingEventData", json("startTime", Long.toString(startTime)),
                "badges", array(json("metadataBadgeRenderer",
                        json("style", "BADGE_STYLE_TYPE_MEMBERS_ONLY"))));
        final YoutubeStreamInfoItemExtractor extractor = new YoutubeStreamInfoItemExtractor(item, null);

        assertEquals(-1, extractor.getDuration());
        assertEquals(Instant.ofEpochSecond(startTime), extractor.getUploadDate().getInstant());
        assertEquals(ContentAvailability.UPCOMING, extractor.getContentAvailability());
        assertEquals(-1, extractor.getViewCount());
    }

    @Test
    void recognizesPremiumContentAndShortFormMetadata() throws Exception {
        final JsonObject item = json(
                "title", json("simpleText", "Short"),
                "badges", array(json("metadataBadgeRenderer", json("label", "Premium"))),
                "viewCountText", json("simpleText", "42 views"),
                "descriptionSnippet", json("simpleText", "Short description"),
                "navigationEndpoint", json("commandMetadata", json("webCommandMetadata",
                        json("webPageType", "WEB_PAGE_TYPE_SHORTS"))));
        final YoutubeStreamInfoItemExtractor extractor = new YoutubeStreamInfoItemExtractor(item, null);

        assertTrue(extractor.isAd());
        assertEquals(-1, extractor.getViewCount());
        assertEquals(ContentAvailability.PAID, extractor.getContentAvailability());
        assertTrue(extractor.isShortFormContent());
        assertEquals("Short description", extractor.getShortDescription());
    }

    @Test
    void readsAlternateDurationAvatarAndShortsRenderers() throws Exception {
        final JsonObject withSeconds = json(
                "title", json("simpleText", "Creator upload"),
                "lengthSeconds", "125",
                "shortBylineText", byline("Creator", "UC123"),
                "channelThumbnail", json("thumbnails", array(thumbnail("alternate.jpg"))));
        final YoutubeStreamInfoItemExtractor seconds =
                new YoutubeStreamInfoItemExtractor(withSeconds, null);
        assertEquals(125, seconds.getDuration());
        assertEquals("Creator", seconds.getUploaderName());
        assertEquals("https://img.example/alternate.jpg",
                seconds.getUploaderAvatars().get(0).getUrl());

        final JsonObject withOverlay = json(
                "title", json("simpleText", "Short clip"),
                "thumbnailOverlays", array(json("thumbnailOverlayTimeStatusRenderer",
                        json("text", json("simpleText", "1:05"), "style", "SHORTS"))));
        final YoutubeStreamInfoItemExtractor overlay =
                new YoutubeStreamInfoItemExtractor(withOverlay, null);
        assertEquals(65, overlay.getDuration());
        assertTrue(overlay.isShortFormContent());
        assertTrue(new YoutubeStreamInfoItemExtractor(
                json("title", json("simpleText", "[Deleted video]")), null).isAd());
        assertThrows(ParsingException.class,
                () -> new YoutubeStreamInfoItemExtractor(new JsonObject(), null).getName());
    }

    private static JsonObject byline(final String name, final String channelId) {
        return json("runs", array(json("text", name, "navigationEndpoint",
                json("browseEndpoint", json("browseId", channelId)))));
    }

    private static JsonObject thumbnail(final String path) {
        return json("url", "https://img.example/" + path, "height", 90, "width", 160);
    }

    private static JsonArray array(final JsonObject... values) {
        return new JsonArray(List.of(values));
    }

    private static JsonObject json(final Object... fields) {
        final JsonObject object = new JsonObject();
        for (int index = 0; index < fields.length; index += 2) {
            object.put((String) fields[index], fields[index + 1]);
        }
        return object;
    }
}
