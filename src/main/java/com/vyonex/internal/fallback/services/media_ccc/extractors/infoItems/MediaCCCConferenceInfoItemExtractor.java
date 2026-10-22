// bxane
package com.vyonex.internal.fallback.services.media_ccc.extractors.infoItems;

import com.grack.nanojson.JsonObject;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.ListExtractor;
import com.vyonex.internal.fallback.channel.ChannelInfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;

import javax.annotation.Nonnull;
import java.util.List;

import static com.vyonex.internal.fallback.services.media_ccc.extractors.MediaCCCParsingHelper.getImageListFromLogoImageUrl;

public class MediaCCCConferenceInfoItemExtractor implements ChannelInfoItemExtractor {
    private final JsonObject conference;

    public MediaCCCConferenceInfoItemExtractor(final JsonObject conference) {
        this.conference = conference;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public long getSubscriberCount() {
        return -1;
    }

    @Override
    public long getStreamCount() {
        return ListExtractor.ITEM_COUNT_UNKNOWN;
    }

    @Override
    public boolean isVerified() throws ParsingException {
        return false;
    }

    @Override
    public String getName() throws ParsingException {
        return conference.getString("title");
    }

    @Override
    public String getUrl() throws ParsingException {
        return conference.getString("url");
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return getImageListFromLogoImageUrl(conference.getString("logo_url"));
    }
}
