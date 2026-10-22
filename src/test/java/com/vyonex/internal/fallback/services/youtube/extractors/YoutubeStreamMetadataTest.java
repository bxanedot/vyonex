// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamExtractor;
import com.vyonex.internal.fallback.stream.StreamType;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeStreamMetadataTest {
    @Test
    void prefersOriginalTitleAndFallsBackToRenderedTitle() throws Exception {
        assertEquals("Original", YoutubeStreamExtractor.extractVideoTitle(
                JsonParser.object().from("{\"title\":\"Original\"}"),
                JsonParser.object().from("{\"title\":{\"simpleText\":\"Rendered\"}}")));
        assertEquals("Rendered", YoutubeStreamExtractor.extractVideoTitle(
                JsonParser.object().from("{}"),
                JsonParser.object().from("{\"title\":{\"simpleText\":\"Rendered\"}}")));
    }

    @Test
    void rejectsResponsesWithoutATitle() throws Exception {
        assertThrows(ParsingException.class, () -> YoutubeStreamExtractor.extractVideoTitle(
                JsonParser.object().from("{}"), JsonParser.object().from("{}")));
    }

    @Test
    void selectsUploadDateFromVideoAndLiveMetadata() throws Exception {
        assertEquals("upload", YoutubeStreamExtractor.extractTextualUploadDate(
                JsonParser.object().from("{\"uploadDate\":\"upload\","
                        + "\"publishDate\":\"published\"}"),
                JsonParser.object().from("{}"), StreamType.VIDEO_STREAM));
        assertEquals("ended", YoutubeStreamExtractor.extractTextualUploadDate(
                JsonParser.object().from("{\"liveBroadcastDetails\":{"
                        + "\"endTimestamp\":\"ended\",\"startTimestamp\":\"started\"}}"),
                JsonParser.object().from("{}"), StreamType.POST_LIVE_STREAM));
    }

    @Test
    void stripsPremiereLabelsAndAllowsUndatedLiveStreams() throws Exception {
        assertEquals("Feb 21, 2020", YoutubeStreamExtractor.extractTextualUploadDate(
                JsonParser.object().from("{}"), JsonParser.object().from(
                        "{\"dateText\":{\"simpleText\":\"Premiered on Feb 21, 2020\"}}"),
                StreamType.VIDEO_STREAM));
        assertNull(YoutubeStreamExtractor.extractTextualUploadDate(JsonParser.object().from("{}"),
                JsonParser.object().from("{}"), StreamType.LIVE_STREAM));
    }

    @Test
    void detectsAgeRestrictionInMetadataRows() throws Exception {
        final var restricted = JsonParser.object().from("{\"metadataRowContainer\":{"
                + "\"metadataRowContainerRenderer\":{\"rows\":[{\"metadataRowRenderer\":{"
                + "\"contents\":[{\"runs\":[{\"text\":\"Age-restricted\"}]}]}}]}}}");
        assertEquals(18, YoutubeStreamExtractor.parseAgeLimit(restricted));
        assertEquals(0, YoutubeStreamExtractor.parseAgeLimit(JsonParser.object().from("{}")));
    }

    @Test
    void prefersDeclaredDurationAndFallsBackToClientFormats() throws Exception {
        final var streamingData = JsonParser.object().from("{\"adaptiveFormats\":["
                + "{\"approxDurationMs\":\"12678\"}]}");
        assertEquals(45, YoutubeStreamExtractor.extractVideoDuration(
                JsonParser.object().from("{\"lengthSeconds\":\"45\"}"),
                Arrays.asList(streamingData)));
        assertEquals(13, YoutubeStreamExtractor.extractVideoDuration(
                JsonParser.object().from("{\"lengthSeconds\":\"invalid\"}"),
                Arrays.asList(null, streamingData)));
    }

    @Test
    void prefersRenderedDescriptionAndUsesPlainTextFallbacks() throws Exception {
        final var rendered = YoutubeStreamExtractor.extractDescription(
                JsonParser.object().from("{\"shortDescription\":\"plain\"}"),
                JsonParser.object().from("{\"description\":{\"simpleText\":\"rendered\"}}"),
                JsonParser.object().from("{}"));
        assertEquals("rendered", rendered.getContent());
        assertEquals(Description.HTML, rendered.getType());

        final var plain = YoutubeStreamExtractor.extractDescription(
                JsonParser.object().from("{\"shortDescription\":\"plain\"}"),
                JsonParser.object().from("{}"), JsonParser.object().from("{}"));
        assertEquals("plain", plain.getContent());
        assertEquals(Description.PLAIN_TEXT, plain.getType());
    }

    @Test
    void readsViewCountFromRendererOrVideoDetails() throws Exception {
        final var primaryInfo = JsonParser.object().from("{\"viewCount\":{"
                + "\"videoViewCountRenderer\":{\"viewCount\":{"
                + "\"simpleText\":\"1,234 views\"}}}}");
        assertEquals(1234, YoutubeStreamExtractor.parseViewCount(primaryInfo,
                JsonParser.object().from("{\"viewCount\":\"567\"}")));
        assertEquals(567, YoutubeStreamExtractor.parseViewCount(JsonParser.object().from("{}"),
                JsonParser.object().from("{\"viewCount\":\"567\"}")));
        assertEquals(0, YoutubeStreamExtractor.parseViewCount(JsonParser.object().from(
                "{\"viewCount\":{\"videoViewCountRenderer\":{\"viewCount\":{"
                        + "\"simpleText\":\"No views\"}}}}"),
                JsonParser.object().from("{\"viewCount\":\"No views\"}")));
        assertThrows(ParsingException.class, () -> YoutubeStreamExtractor.parseViewCount(
                JsonParser.object().from("{}"), JsonParser.object().from("{}")));
    }

    @Test
    void readsUploaderNameFromVideoDetails() throws Exception {
        assertEquals("Artist", YoutubeStreamExtractor.extractUploaderName(
                JsonParser.object().from("{\"author\":\"Artist\"}")));
        assertThrows(ParsingException.class, () -> YoutubeStreamExtractor.extractUploaderName(
                JsonParser.object().from("{}")));
    }

    @Test
    void buildsUploaderUrlFromChannelId() throws Exception {
        assertEquals("https://www.youtube.com/channel/UC123",
                YoutubeStreamExtractor.extractUploaderUrl(
                        JsonParser.object().from("{\"channelId\":\"UC123\"}")));
        assertThrows(ParsingException.class, () -> YoutubeStreamExtractor.extractUploaderUrl(
                JsonParser.object().from("{}")));
    }

    @Test
    void readsUploaderAvatarAndAllowsMissingImageForAgeRestrictedVideo() throws Exception {
        final var owner = JsonParser.object().from("{\"thumbnail\":{\"thumbnails\":[{"
                + "\"url\":\"https://img.example/avatar.jpg\"}]}}");
        final var avatars = YoutubeStreamExtractor.extractUploaderAvatars(owner, 0);
        assertEquals("https://img.example/avatar.jpg", avatars.get(0).getUrl());
        assertThrows(ParsingException.class, () -> YoutubeStreamExtractor.extractUploaderAvatars(
                JsonParser.object().from("{}"), 0));
        assertEquals(java.util.List.of(), YoutubeStreamExtractor.extractUploaderAvatars(
                JsonParser.object().from("{}"), 18));
    }

    @Test
    void readsSubscriberCountFromOwnerOrCollaborator() throws Exception {
        assertEquals(1_200_000, YoutubeStreamExtractor.extractUploaderSubscriberCount(
                JsonParser.object().from("{\"subscriberCountText\":{"
                        + "\"simpleText\":\"1.2M subscribers\"}}")));
        assertEquals(12_000, YoutubeStreamExtractor.extractUploaderSubscriberCount(
                JsonParser.object().from("{\"navigationEndpoint\":{\"showDialogCommand\":{"
                        + "\"panelLoadingStrategy\":{\"inlineContent\":{"
                        + "\"dialogViewModel\":{\"customContent\":{\"listViewModel\":{"
                        + "\"listItems\":[{\"listItemViewModel\":{\"subtitle\":{"
                        + "\"content\":\"Artist • 12K subscribers\"}}}]}}}}}}}}")));
        assertEquals(StreamExtractor.UNKNOWN_SUBSCRIBER_COUNT,
                YoutubeStreamExtractor.extractUploaderSubscriberCount(
                        JsonParser.object().from("{}")));
    }
}
