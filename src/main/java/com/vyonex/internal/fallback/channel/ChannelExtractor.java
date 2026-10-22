/*
 * Created by Christian Schabesberger on 25.07.16.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * ChannelExtractor.java is part of VyonEx.
 *
 * VyonEx is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VyonEx is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VyonEx.  If not, see <https://www.gnu.org/licenses/>.
 */

// bxane
package com.vyonex.internal.fallback.channel;

import com.vyonex.internal.fallback.Extractor;
import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class ChannelExtractor extends Extractor {

    public static final long UNKNOWN_SUBSCRIBER_COUNT = -1;

    protected ChannelExtractor(final StreamingService service, final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Nonnull
    public abstract List<Image> getAvatars() throws ParsingException;
    @Nonnull
    public abstract List<Image> getBanners() throws ParsingException;
    public abstract String getFeedUrl() throws ParsingException;
    public abstract long getSubscriberCount() throws ParsingException;
    public abstract String getDescription() throws ParsingException;
    public abstract String getParentChannelName() throws ParsingException;
    public abstract String getParentChannelUrl() throws ParsingException;
    @Nonnull
    public abstract List<Image> getParentChannelAvatars() throws ParsingException;
    public abstract boolean isVerified() throws ParsingException;
    @Nonnull
    public abstract List<ListLinkHandler> getTabs() throws ParsingException;
    @Nonnull
    public List<String> getTags() throws ParsingException {
        return List.of();
    }
}
