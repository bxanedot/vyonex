// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.playlist.PlaylistInfo;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeMixOrPlaylistLockupInfoItemExtractorContractTest {
    @Test
    void mapsPlaylistLockupMetadataAndCounts() throws Exception {
        final var extractor = new YoutubeMixOrPlaylistLockupInfoItemExtractor(JsonParser.object().from("""
                {
                  "contentId":"PLabcdefghijk",
                  "contentImage":{"collectionThumbnailViewModel":{"primaryThumbnail":{"thumbnailViewModel":{
                    "image":{"sources":[{"url":"//img.example/lockup.jpg","width":640,"height":360}]},
                    "overlays":[{"thumbnailOverlayBadgeViewModel":{"thumbnailBadges":[
                      {"thumbnailBadgeViewModel":{"text":"1.2K videos"}}
                    ]}}]
                  }}}},
                  "metadata":{"lockupMetadataViewModel":{"title":{"content":"Course list"},"metadata":{"contentMetadataViewModel":{
                    "metadataRows":[{"metadataParts":[{"text":{"content":"Creator","commandRuns":[{"onTap":{"innertubeCommand":{"browseEndpoint":{"browseId":"UC123"}}}}],"attachmentRuns":[{"element":{"type":{"imageType":{"image":{"sources":[{"clientResource":{"imageName":"CHECK_CIRCLE_FILLED"}}]}}}}}]}}]}]
                  }}}},
                  "rendererContext":{"commandContext":{"onTap":{"innertubeCommand":{"browseEndpoint":{"browseId":"VLPLabcdefghijk"}}},"watchEndpoint":{"playlistId":"PLabcdefghijk"}}}
                }
                """));

        assertEquals("Course list", extractor.getName());
        assertEquals("https://www.youtube.com/playlist?list=PLabcdefghijk", extractor.getUrl());
        assertEquals("https://img.example/lockup.jpg", extractor.getThumbnails().get(0).getUrl());
        assertEquals("Creator", extractor.getUploaderName());
        assertEquals("https://www.youtube.com/channel/UC123", extractor.getUploaderUrl());
        assertTrue(extractor.isUploaderVerified());
        assertEquals(1200, extractor.getStreamCount());
        assertEquals(PlaylistInfo.PlaylistType.NORMAL, extractor.getPlaylistType());
    }

    @Test
    void mapsMixLockupsToWatchUrlsWithoutUploaderOrFiniteCount() throws Exception {
        final var extractor = new YoutubeMixOrPlaylistLockupInfoItemExtractor(JsonParser.object().from("""
                {
                  "contentId":"RDMMabcdefghijk",
                  "metadata":{"lockupMetadataViewModel":{"title":{"content":"Mix"},"metadata":{"contentMetadataViewModel":{"metadataRows":[]}}}},
                  "rendererContext":{"commandContext":{"onTap":{"innertubeCommand":{"watchEndpoint":{"videoId":"abcdefghijk","playlistId":"RDMMabcdefghijk"}}},"watchEndpoint":{"playlistId":"RDMMabcdefghijk"}}}
                }
                """));

        assertEquals(PlaylistInfo.PlaylistType.MIX_STREAM, extractor.getPlaylistType());
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk&list=RDMMabcdefghijk", extractor.getUrl());
        assertNull(extractor.getUploaderUrl());
        assertFalse(extractor.isUploaderVerified());
        assertEquals(ListExtractor.ITEM_COUNT_INFINITE, extractor.getStreamCount());
    }
}
