// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.fallback.channel.ChannelInfoItem;
import com.vyonex.internal.fallback.channel.ChannelInfoItemsCollector;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudService;
import com.vyonex.internal.fallback.subscription.SubscriptionExtractor;
import com.vyonex.internal.fallback.subscription.SubscriptionItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL;
import static com.vyonex.internal.fallback.utils.Utils.HTTPS;
import static com.vyonex.internal.fallback.utils.Utils.replaceHttpWithHttps;

/**
 * Extract the "followings" from a user in SoundCloud.
 */
public class SoundcloudSubscriptionExtractor extends SubscriptionExtractor {

    public SoundcloudSubscriptionExtractor(final SoundcloudService service) {
        super(service, Collections.singletonList(ContentSource.CHANNEL_URL));
    }

    @Override
    public String getRelatedUrl() {
        return "https://soundcloud.com/you";
    }

    @Override
    public List<SubscriptionItem> fromChannelUrl(final String channelUrl) throws IOException,
            ExtractionException {
        if (channelUrl == null) {
            throw new InvalidSourceException("Channel url is null");
        }

        final String id;
        try {
            id = service.getChannelLHFactory().fromUrl(getUrlFrom(channelUrl)).getId();
        } catch (final ExtractionException e) {
            throw new InvalidSourceException(e);
        }

        final String apiUrl = SOUNDCLOUD_API_V2_URL + "users/" + id + "/followings" + "?client_id="
                + SoundcloudParsingHelper.clientId() + "&limit=200";
        final ChannelInfoItemsCollector collector = new ChannelInfoItemsCollector(service
                .getServiceId());
        // ± 2000 is the limit of followings on SoundCloud, so this minimum should be enough
        SoundcloudParsingHelper.getUsersFromApiMinItems(2500, collector, apiUrl);

        return toSubscriptionItems(collector.getItems());
    }

    private String getUrlFrom(final String channelUrl) {
        final String fixedUrl = replaceHttpWithHttps(channelUrl);
        if (fixedUrl.startsWith(HTTPS)) {
            return channelUrl;
        } else if (!fixedUrl.contains("soundcloud.com/")) {
            return "https://soundcloud.com/" + fixedUrl;
        } else {
            return HTTPS + fixedUrl;
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    private List<SubscriptionItem> toSubscriptionItems(final List<ChannelInfoItem> items) {
        final List<SubscriptionItem> result = new ArrayList<>(items.size());
        for (final ChannelInfoItem item : items) {
            result.add(new SubscriptionItem(item.getServiceId(), item.getUrl(), item.getName()));
        }
        return result;
    }
}
