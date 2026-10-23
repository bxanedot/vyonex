// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.utils.Utils;
import com.vyonex.internal.json.JsonObject;

import java.util.List;

import javax.annotation.Nonnull;

public class YoutubeChannelInfoItemExtractor implements ChannelInfoItemExtractor {
    private final JsonObject channel;
    private final boolean handleInSubscriberField;

    public YoutubeChannelInfoItemExtractor(final JsonObject channelInfoItem) {
        channel = channelInfoItem;
        final String subscriberLabel = YoutubeParsingHelper.getTextFromObject(
                channel.getObject("subscriberCountText"));
        handleInSubscriberField = subscriberLabel != null && subscriberLabel.startsWith("@");
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        try {
            return YoutubeParsingHelper.getThumbnailsFromInfoItem(channel);
        } catch (final Exception error) {
            throw new ParsingException("Could not read channel thumbnails", error);
        }
    }

    @Override
    public String getName() throws ParsingException {
        return YoutubeParsingHelper.getTextFromObject(channel.getObject("title"));
    }

    @Override
    public String getUrl() throws ParsingException {
        final String id = channel.getString("channelId");
        if (id == null || id.isBlank()) throw new ParsingException("Channel ID is missing");
        try {
            return YoutubeChannelLinkHandlerFactory.getInstance()
                    .getUrl("channel/" + id);
        } catch (final RuntimeException e) {
            throw new ParsingException("Could not read channel URL", e);
        }
    }

    @Override
    public long getSubscriberCount() throws ParsingException {
        if (!channel.has("subscriberCountText")) return -1;
        final String value = YoutubeParsingHelper.getTextFromObject(channel.getObject(
                handleInSubscriberField ? "videoCountText" : "subscriberCountText"));
        if (value == null || value.isBlank()) {
            throw new ParsingException("Could not read subscriber count");
        }
        try {
            return Utils.mixedNumberWordToLong(value);
        } catch (final ParsingException | RuntimeException e) {
            throw new ParsingException("Could not parse subscriber count", e);
        }
    }

    @Override
    public long getStreamCount() throws ParsingException {
        if (handleInSubscriberField || !channel.has("videoCountText")) {
            return ListExtractor.ITEM_COUNT_UNKNOWN;
        }
        final String value = YoutubeParsingHelper.getTextFromObject(channel.getObject("videoCountText"));
        if (value == null || value.isBlank()) {
            throw new ParsingException("Could not parse channel video count");
        }
        try {
            return Long.parseLong(Utils.removeNonDigitCharacters(value));
        } catch (final RuntimeException e) {
            throw new ParsingException("Could not parse channel video count", e);
        }
    }

    @Override
    public boolean isVerified() {
        return YoutubeParsingHelper.isVerified(channel.getArray("ownerBadges"));
    }

    @Override
    public String getDescription() {
        return channel.has("descriptionSnippet")
                ? YoutubeParsingHelper.getTextFromObject(channel.getObject("descriptionSnippet"))
                : null;
    }
}
