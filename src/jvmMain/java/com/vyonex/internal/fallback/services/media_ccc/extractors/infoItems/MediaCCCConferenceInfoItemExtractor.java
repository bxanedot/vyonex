// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors.infoItems;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCConferenceSummaryPayload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper.getImageListFromLogoImageUrl;

public class MediaCCCConferenceInfoItemExtractor implements ChannelInfoItemExtractor {
    @Nullable private final String title;
    @Nullable private final String url;
    private final List<Image> thumbnails;

    public MediaCCCConferenceInfoItemExtractor(final MediaCCCConferenceSummaryPayload conference) {
        title = conference.getTitle();
        url = conference.getUrl();
        thumbnails = getImageListFromLogoImageUrl(conference.getLogoUrl());
    }

    @Override public String getDescription() { return ""; }
    @Override public long getSubscriberCount() { return -1; }
    @Override public long getStreamCount() { return ListExtractor.ITEM_COUNT_UNKNOWN; }
    @Override public boolean isVerified() { return false; }
    @Override public String getName() throws ParsingException { return title; }
    @Override public String getUrl() throws ParsingException { return url; }
    @Nonnull @Override public List<Image> getThumbnails() { return thumbnails; }
}
