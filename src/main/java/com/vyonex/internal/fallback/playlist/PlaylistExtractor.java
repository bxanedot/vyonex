// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.playlist;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.fallback.stream.StreamInfoItem;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class PlaylistExtractor extends ListExtractor<StreamInfoItem> {
    public PlaylistExtractor(final StreamingService service, final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    public abstract String getUploaderUrl() throws ParsingException;
    public abstract String getUploaderName() throws ParsingException;

    @Nonnull
    public abstract List<Image> getUploaderAvatars() throws ParsingException;

    public abstract boolean isUploaderVerified() throws ParsingException;
    public abstract long getStreamCount() throws ParsingException;

    @Nonnull
    public abstract Description getDescription() throws ParsingException;

    @Nonnull
    public List<Image> getThumbnails() throws ParsingException { return List.of(); }

    @Nonnull
    public List<Image> getBanners() throws ParsingException { return List.of(); }

    @Nonnull
    public String getSubChannelName() throws ParsingException { return ""; }

    @Nonnull
    public String getSubChannelUrl() throws ParsingException { return ""; }

    @Nonnull
    public List<Image> getSubChannelAvatars() throws ParsingException { return List.of(); }

    public PlaylistInfo.PlaylistType getPlaylistType() throws ParsingException {
        return PlaylistInfo.PlaylistType.NORMAL;
    }
}
