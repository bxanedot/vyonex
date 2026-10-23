// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.ibm.icu.util.ULocale;
import com.vyonex.internal.fallback.comments.CommentsInfoItem;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.TimeAgoParser;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeCommentsInfoItemExtractorContractTest {
    private final TimeAgoParser dates = new TimeAgoParser(ULocale.ENGLISH,
            LocalDateTime.of(2026, 10, 23, 12, 0));

    @Test
    void mapsCommentTextAuthorCountsBadgesAndReplies() throws Exception {
        final var comment = JsonParser.object().from("""
                {
                  "commentId":"comment-1",
                  "authorText":{"simpleText":"Creator"},
                  "authorEndpoint":{"browseEndpoint":{"browseId":"UC123"}},
                  "authorThumbnail":{"thumbnails":[{"url":"//img.example/creator.jpg","width":48,"height":48}]},
                  "publishedTimeText":{"simpleText":"2 days ago"},
                  "voteCount":{"simpleText":"11"},
                  "contentText":{"simpleText":"\\uFEFFGreat video"},
                  "actionButtons":{"commentActionButtonsRenderer":{
                    "creatorHeart":{},
                    "likeButton":{"toggleButtonRenderer":{"accessibilityData":{"accessibilityData":{"label":"12 likes"}}}}
                  }},
                  "replyCount":3,
                  "authorIsChannelOwner":true,
                  "authorCommentBadge":{},
                  "pinnedCommentBadge":{}
                }
                """);
        final var replies = JsonParser.object().from("""
                {"viewRepliesCreatorThumbnail":{},"contents":[{"continuationItemRenderer":{"continuationEndpoint":{"continuationCommand":{"token":"reply-token"}}}}]}
                """);
        final var extractor = new YoutubeCommentsInfoItemExtractor(comment, replies,
                "https://www.youtube.com/watch?v=abcdefghijk", dates);

        assertEquals("Creator", extractor.getName());
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", extractor.getUrl());
        assertEquals("Creator", extractor.getUploaderName());
        assertEquals("https://www.youtube.com/channel/UC123", extractor.getUploaderUrl());
        assertEquals("https://img.example/creator.jpg", extractor.getThumbnails().get(0).getUrl());
        assertEquals(12, extractor.getLikeCount());
        assertEquals("11", extractor.getTextualLikeCount());
        assertEquals("comment-1", extractor.getCommentId());
        assertTrue(extractor.getCommentText().getContent().contains("Great"));
        assertFalse(extractor.getCommentText().getContent().contains("\uFEFF"));
        assertEquals("2 days ago", extractor.getTextualUploadDate());
        assertNotNull(extractor.getUploadDate());
        assertTrue(extractor.isHeartedByUploader());
        assertTrue(extractor.isPinned());
        assertTrue(extractor.isUploaderVerified());
        assertTrue(extractor.isChannelOwner());
        assertEquals(3, extractor.getReplyCount());
        assertEquals("reply-token", extractor.getReplies().getId());
        assertTrue(extractor.hasCreatorReply());
    }

    @Test
    void fallsBackToVisibleLikeCountAndHandlesMissingOptionalData() throws Exception {
        final var comment = JsonParser.object().from("""
                {"voteCount":{"simpleText":"1.2K"},"contentText":{}}
                """);
        final var extractor = new YoutubeCommentsInfoItemExtractor(comment, null,
                "https://www.youtube.com/watch?v=abcdefghijk", dates);

        assertEquals(1200, extractor.getLikeCount());
        assertEquals(CommentsInfoItem.UNKNOWN_REPLY_COUNT, extractor.getReplyCount());
        assertEquals("", extractor.getName());
        assertEquals("", extractor.getUploaderUrl());
        assertNull(extractor.getReplies());
        assertFalse(extractor.hasCreatorReply());
        assertEquals("", extractor.getCommentText().getContent());
        assertThrows(ParsingException.class, extractor::getCommentId);
        assertThrows(ParsingException.class, extractor::getTextualUploadDate);
    }
}
