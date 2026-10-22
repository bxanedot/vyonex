// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor;

import javax.annotation.Nonnull;
import java.util.List;

import static com.vyonex.internal.fallback.services.soundcloud.SoundcloudParsingHelper.getAllImagesFromArtworkOrAvatarUrl;
import static com.vyonex.internal.fallback.utils.Utils.replaceHttpWithHttps;

public class SoundcloudChannelInfoItemExtractor implements ChannelInfoItemExtractor {
    private final JsonObject itemObject;

    public SoundcloudChannelInfoItemExtractor(final JsonObject itemObject) {
        this.itemObject = itemObject;
    }

    @Override
    public String getName() {
        return itemObject.getString("username");
    }

    @Override
    public String getUrl() {
        return replaceHttpWithHttps(itemObject.getString("permalink_url"));
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return getAllImagesFromArtworkOrAvatarUrl(itemObject.getString("avatar_url"));
    }

    @Override
    public long getSubscriberCount() {
        return itemObject.getLong("followers_count");
    }

    @Override
    public long getStreamCount() {
        return itemObject.getLong("track_count");
    }

    @Override
    public boolean isVerified() {
        return itemObject.getBoolean("verified");
    }

    @Override
    public String getDescription() {
        return itemObject.getString("description", "");
    }
}
