// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.ibm.icu.util.ULocale;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeCommentsEUVMInfoItemExtractorContractTest {
    private final TimeAgoParser dates = new TimeAgoParser(ULocale.ENGLISH,
            LocalDateTime.of(2026, 10, 23, 12, 0));

    @Test
    void mapsCommentEntityAndEngagement() throws Exception {
        final var extractor = new YoutubeCommentsEUVMInfoItemExtractor(
                JsonParser.object().from("{\"commentId\":\"view-id\",\"pinnedText\":\"Pinned\"}"),
                JsonParser.object().from("""
                        {
                          "viewRepliesCreatorThumbnail":{},
                          "contents":[{"continuationItemRenderer":{
                            "continuationEndpoint":{"continuationCommand":{"token":"replies-next"}}
                          }}]
                        }
                        """),
                JsonParser.object().from("""
                        {
                          "properties":{"commentId":"entity-id","publishedTime":"3 days ago","content":{"content":"Great clip"}},
                          "toolbar":{"likeCountNotliked":"1.2K","replyCount":"1.5K"},
                          "author":{"displayName":"Creator","channelId":"UC789","isVerified":true,"isArtist":false,"isCreator":true},
                          "avatar":{"image":{"sources":[{"url":"//img.example/creator.jpg","width":48,"height":48}]}}
                        }
                        """),
                JsonParser.object().from("{\"heartState\":\"TOOLBAR_HEART_STATE_HEARTED\"}"),
                "https://www.youtube.com/watch?v=abcdefghijk", dates);

        assertEquals("Creator", extractor.getName());
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", extractor.getUrl());
        assertEquals("Creator", extractor.getUploaderName());
        assertEquals("https://www.youtube.com/channel/UC789", extractor.getUploaderUrl());
        assertEquals("https://img.example/creator.jpg", extractor.getThumbnails().get(0).getUrl());
        assertEquals(1200, extractor.getLikeCount());
        assertEquals("1.2K", extractor.getTextualLikeCount());
        assertEquals("Great clip", extractor.getCommentText().getContent());
        assertEquals("entity-id", extractor.getCommentId());
        assertEquals("3 days ago", extractor.getTextualUploadDate());
        assertTrue(extractor.getUploadDate().getInstant().isBefore(
                LocalDateTime.of(2026, 10, 23, 12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant()));
        assertEquals(1500, extractor.getReplyCount());
        assertEquals("replies-next", extractor.getReplies().getId());
        assertTrue(extractor.isHeartedByUploader());
        assertTrue(extractor.isPinned());
        assertTrue(extractor.isUploaderVerified());
        assertTrue(extractor.isChannelOwner());
        assertTrue(extractor.hasCreatorReply());
    }

    @Test
    void supportsFallbackAuthorRouteAndCommentId() throws Exception {
        final var extractor = new YoutubeCommentsEUVMInfoItemExtractor(
                JsonParser.object().from("{\"commentId\":\"view-id\"}"), null,
                JsonParser.object().from("""
                        {"author":{"displayName":"Artist","isArtist":true,"channelCommand":{"innertubeCommand":{"browseEndpoint":{"browseId":"UC456"}}}}}
                        """), JsonParser.object().from("{}"),
                "https://www.youtube.com/watch?v=abcdefghijk", dates);

        assertEquals("view-id", extractor.getCommentId());
        assertEquals("https://www.youtube.com/channel/UC456", extractor.getUploaderUrl());
        assertTrue(extractor.isUploaderVerified());
        assertFalse(extractor.isHeartedByUploader());
        assertNull(extractor.getReplies());
        assertFalse(extractor.hasCreatorReply());
        assertEquals(0, extractor.getLikeCount());
        assertEquals(0, extractor.getReplyCount());
        assertThrows(ParsingException.class,
                () -> new YoutubeCommentsEUVMInfoItemExtractor(JsonParser.object().from("{}"),
                        JsonParser.object().from("{}"), JsonParser.object().from("{}"),
                        JsonParser.object().from("{}"), "https://www.youtube.com/watch?v=x", dates)
                        .getReplies());
    }
}
