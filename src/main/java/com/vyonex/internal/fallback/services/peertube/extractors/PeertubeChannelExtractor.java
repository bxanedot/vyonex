// bxane
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory;
import com.vyonex.internal.fallback.utils.JsonUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getBannersFromAccountOrVideoChannelObject;

public class PeertubeChannelExtractor extends ChannelExtractor {
    private JsonObject json;
    private final String baseUrl;

    public PeertubeChannelExtractor(final StreamingService service,
                                    final ListLinkHandler linkHandler) throws ParsingException {
        super(service, linkHandler);
        this.baseUrl = getBaseUrl();
    }

    @Nonnull
    @Override
    public List<Image> getAvatars() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, json);
    }

    @Nonnull
    @Override
    public List<Image> getBanners() {
        return getBannersFromAccountOrVideoChannelObject(baseUrl, json);
    }

    @Override
    public String getFeedUrl() throws ParsingException {
        return getBaseUrl() + "/feeds/videos.xml?videoChannelId=" + json.get("id");
    }

    @Override
    public long getSubscriberCount() {
        return json.getLong("followersCount");
    }

    @Nullable
    @Override
    public String getDescription() {
        return json.getString("description");
    }

    @Override
    public String getParentChannelName() throws ParsingException {
        return JsonUtils.getString(json, "ownerAccount.name");
    }

    @Override
    public String getParentChannelUrl() throws ParsingException {
        return JsonUtils.getString(json, "ownerAccount.url");
    }

    @Nonnull
    @Override
    public List<Image> getParentChannelAvatars() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(
                baseUrl, json.getObject("ownerAccount"));
    }

    @Override
    public boolean isVerified() throws ParsingException {
        return false;
    }

    @Nonnull
    @Override
    public List<ListLinkHandler> getTabs() throws ParsingException {
        return List.of(
                PeertubeChannelTabLinkHandlerFactory.getInstance().fromQuery(getId(),
                        List.of(ChannelTabs.VIDEOS), "", getBaseUrl()),
                PeertubeChannelTabLinkHandlerFactory.getInstance().fromQuery(getId(),
                        List.of(ChannelTabs.PLAYLISTS), "", getBaseUrl()));
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final Response response = downloader.get(
                baseUrl + PeertubeChannelLinkHandlerFactory.API_ENDPOINT + getId());
        if (response != null) {
            setInitialData(response.responseBody());
        } else {
            throw new ExtractionException("Unable to extract PeerTube channel data");
        }
    }

    private void setInitialData(final String responseBody) throws ExtractionException {
        try {
            json = JsonParser.object().from(responseBody);
        } catch (final JsonParserException e) {
            throw new ExtractionException("Unable to extract PeerTube channel data", e);
        }
        if (json == null) {
            throw new ExtractionException("Unable to extract PeerTube channel data");
        }
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return JsonUtils.getString(json, "displayName");
    }
}
