// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.channel.ChannelInfoItemsCollector;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudService;
import com.vyonex.internal.fallback.subscription.SubscriptionExtractor;
import com.vyonex.internal.fallback.subscription.SubscriptionItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SoundcloudSubscriptionExtractor extends SubscriptionExtractor {
    public SoundcloudSubscriptionExtractor(final SoundcloudService service) {
        super(service, List.of(ContentSource.CHANNEL_URL));
    }

    @Override public String getRelatedUrl() { return "https://soundcloud.com/you"; }

    @Override public List<SubscriptionItem> fromChannelUrl(final String channelUrl)
            throws IOException, ExtractionException {
        if (channelUrl == null || channelUrl.isBlank()) {
            throw new InvalidSourceException("Channel URL is empty");
        }
        final String url = normalizeChannelUrl(channelUrl);
        final String id;
        try {
            id = service.getChannelLHFactory().getId(url);
        } catch (final ExtractionException e) {
            throw new InvalidSourceException(e);
        }
        final String endpoint = SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
                + "users/" + id + "/followings?client_id="
                + SoundcloudParsingHelper.clientId() + "&limit=200";
        final ChannelInfoItemsCollector users = new ChannelInfoItemsCollector(service.getServiceId());
        SoundcloudParsingHelper.getUsersFromApiMinItems(2500, users, endpoint);
        final List<SubscriptionItem> subscriptions = new ArrayList<>(users.getItems().size());
        for (final var user : users.getItems()) {
            subscriptions.add(new SubscriptionItem(user.getServiceId(), user.getUrl(), user.getName()));
        }
        return subscriptions;
    }

    private static String normalizeChannelUrl(final String channelUrl) {
        final String value = channelUrl.trim();
        if (value.contains("://")) return value;
        if (value.matches("(?i)^(?:www\\.|m\\.)?soundcloud\\.com(?:/.*)?$")) {
            return "https://" + value;
        }
        return "https://soundcloud.com/" + value.replaceFirst("^/+", "");
    }
}
